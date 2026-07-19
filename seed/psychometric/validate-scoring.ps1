# Accuracy validation for psychometric scoring. Submits known-profile fixtures
# through the real API (band Class 11-12) and asserts the report matches
# expectation. Writes docs/psychometric-validation.md. Idempotent (each run
# creates fresh attempts). Run seed-banks.ps1 first.
param(
  [string]$BaseUrl = "http://localhost:8081",
  [string]$Pass = "seedpass123"
)
$ErrorActionPreference = "Stop"
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $here "..\..")

$index = Get-Content (Join-Path $here "seeded-index.json") -Raw -Encoding UTF8 | ConvertFrom-Json
$band = $index."Class 11-12"
$quizId = $band.quizId

# dimension -> list of quesIds  (ConvertFrom-Json gives PSCustomObject; flatten)
$qbyDim = @{}
foreach ($p in $band.questions.PSObject.Properties) { $qbyDim[$p.Name] = @($p.Value) }

$student = Invoke-RestMethod "$BaseUrl/api/login" -Method Post -ContentType "application/json" `
  -Body (@{ username = "psychobankstudent"; password = $Pass } | ConvertTo-Json)
$uh = @{ Authorization = "Bearer $($student.jwtToken)" }

$MI = @("LOGICAL","MUSICAL","NATURALIST","VERBAL","INTERPERSONAL","KINESTHETIC","SPATIAL","INTRAPERSONAL","EXISTENTIAL")

# Submit a fixture (spikeDims -> spikeOpt, everything else baseOpt) and return the report.
function RunFixture($spikeDims, $baseOpt, $spikeOpt) {
  $ans = @{}
  foreach ($dim in $qbyDim.Keys) {
    $opt = if ($spikeDims -contains $dim) { $spikeOpt } else { $baseOpt }
    foreach ($qid in $qbyDim[$dim]) { $ans["$qid"] = $opt }
  }
  $res = Invoke-RestMethod "$BaseUrl/api/quizResult/submit/?quizId=$quizId" -Method Post `
    -ContentType "application/json" -Headers $uh -Body ($ans | ConvertTo-Json)
  Invoke-RestMethod "$BaseUrl/api/psychometric-report/$($res.quizResId)" -Headers $uh
}

$results = New-Object System.Collections.ArrayList
function Check($name, $inputDesc, $expected, $actual, $pass) {
  [void]$results.Add([pscustomobject]@{ Name=$name; Input=$inputDesc; Expected=$expected; Actual=$actual; Pass=$pass })
  $tag = if ($pass) { "PASS" } else { "FAIL" }
  Write-Host "[$tag] $name -- $actual"
}
function MiPct($rep, $dim) { ($rep.multipleIntelligences | Where-Object { $_.dimension -eq $dim }).percent }
function Rank1($rep) { ($rep.multipleIntelligences | Sort-Object rank | Select-Object -First 1).dimension }
function Quot($rep, $code) { ($rep.quotients | Where-Object { $_.code -eq $code }).percent }
function TopFields($rep, $n) { ($rep.careers | Select-Object -First $n | ForEach-Object { $_.field }) }

# ---- MI single-dimension spikes: expect that dimension rank #1 + expected quotient/careers ----
$miSpikes = @(
  @{ dim="LOGICAL";       quot="IQ"; careers=@("Engineering & Technology","Data & Research Science","Finance & Accounting") },
  @{ dim="VERBAL";        quot="IQ"; careers=@("Media & Communication","Law & Public Policy","Education & Social Work") },
  @{ dim="INTERPERSONAL"; quot="EQ"; careers=@("Business & Management","Education & Social Work","Medicine & Health Care") },
  @{ dim="KINESTHETIC";   quot="AQ"; careers=@("Sports & Physical Sciences","Performing Arts & Music") }
)
foreach ($s in $miSpikes) {
  $rep = RunFixture @($s.dim) "option2" "option4"
  $r1 = Rank1 $rep
  Check "MI spike: $($s.dim) ranks #1" "all $($s.dim)=agree(4), rest=disagree(2)" "$($s.dim) rank1" "rank1=$r1 ($([math]::Round((MiPct $rep $s.dim),1))%)" ($r1 -eq $s.dim)

  $q = Quot $rep $s.quot
  Check "MI spike: $($s.dim) -> $($s.quot) top quotient" "same fixture" "$($s.quot)=100 (max)" "$($s.quot)=$q" ($q -eq 100)

  $top3 = TopFields $rep 3
  $hit = ($top3 | Where-Object { $s.careers -contains $_ })
  Check "MI spike: $($s.dim) career leaning" "same fixture" "top-3 includes one of: $($s.careers -join ', ')" "top3=$($top3 -join ' | ')" ([bool]$hit)
}

# ---- RIASEC spike: R dominant + realistic careers ----
$repR = RunFixture @("R") "option2" "option4"
$rDom = ($repR.riasec | Where-Object { $_.letter -eq "R" }).dominant
$holland = $repR.hollandCode
Check "RIASEC spike: R is dominant" "all R=agree(4), rest=disagree(2)" "R dominant, Holland starts R" "R.dominant=$rDom, Holland=$holland" ($rDom -and $holland.StartsWith("R"))
$rCareers = @("Engineering & Technology","Sports & Physical Sciences","Environment & Agriculture")
$topR = TopFields $repR 3
$rHit = ($topR | Where-Object { $rCareers -contains $_ })
Check "RIASEC spike: R hands-on careers on top" "same fixture" "top-3 includes one of: $($rCareers -join ', ')" "top3=$($topR -join ' | ')" ([bool]$rHit)

# ---- Sum sanity (on the LOGICAL spike profile) ----
$repL = RunFixture @("LOGICAL") "option2" "option4"
$miSum = [math]::Round((($repL.multipleIntelligences | Measure-Object percent -Sum).Sum), 1)
Check "Sum sanity: MI percents total ~100" "LOGICAL spike" "sum in 99-101" "sum=$miSum" ([math]::Abs($miSum - 100) -lt 1.0)

$miMap = @{}; foreach ($m in $MI) { $miMap[$m] = MiPct $repL $m }
$analytical = ($repL.domains | Where-Object { $_.name -eq "Analytical" }).percent
$expAnalytical = [math]::Round($miMap["LOGICAL"] + $miMap["MUSICAL"] + $miMap["NATURALIST"], 1)
Check "Sum sanity: Analytical domain = fixed sum" "LOGICAL spike" "Analytical=$expAnalytical" "Analytical=$analytical" ([math]::Abs($analytical - $expAnalytical) -lt 0.2)

$ranks = ($repL.multipleIntelligences | ForEach-Object { $_.rank } | Sort-Object)
$validRanks = (($ranks -join ",") -eq (1..9 -join ","))
Check "Sum sanity: ranks are a 1..9 permutation" "LOGICAL spike" "1..9 each once" "ranks=$($ranks -join ',')" $validRanks

# ---- Boundary: all answers identical -> no crash, even MI distribution ----
$repN = RunFixture @() "option2" "option2"
$vals = $MI | ForEach-Object { MiPct $repN $_ }
$spread = ([math]::Round(($vals | Measure-Object -Maximum).Maximum - ($vals | Measure-Object -Minimum).Minimum, 2))
$nRanks = ($repN.multipleIntelligences | ForEach-Object { $_.rank } | Sort-Object)
$nValid = (($nRanks -join ",") -eq (1..9 -join ","))
Check "Boundary: uniform answers -> even MI + no crash" "every item=disagree(2)" "spread<2 and ranks valid" "spread=$spread, ranksValid=$nValid" (($spread -lt 2.0) -and $nValid)

# ---------- write docs/psychometric-validation.md ----------
$pass = ($results | Where-Object { $_.Pass }).Count
$total = $results.Count
$now = Get-Date -Format "yyyy-MM-dd HH:mm"
$sb = New-Object System.Text.StringBuilder
[void]$sb.AppendLine("# Psychometric Scoring - Accuracy Validation")
[void]$sb.AppendLine("")
[void]$sb.AppendLine("Automated fixtures submitted through the real API (band **Class 11-12**, quizId $quizId) as")
[void]$sb.AppendLine("student ``psychobankstudent``; each row asserts the persisted report from")
[void]$sb.AppendLine("``GET /api/psychometric-report/{quizResId}``. Regenerate with ``validate-scoring.ps1``.")
[void]$sb.AppendLine("")
[void]$sb.AppendLine("**Result: $pass / $total passed** (run $now)")
[void]$sb.AppendLine("")
[void]$sb.AppendLine("Scale: each item answered ``Strongly agree`` = 4 (spike) or ``Disagree`` = 2 (baseline).")
[void]$sb.AppendLine("")
[void]$sb.AppendLine("| # | Fixture | Input | Expected | Actual | Result |")
[void]$sb.AppendLine("|---|---------|-------|----------|--------|--------|")
$i = 1
foreach ($r in $results) {
  $tag = if ($r.Pass) { "PASS" } else { "**FAIL**" }
  # Escape pipes so career lists ("A | B | C") don't break the markdown table.
  $inp = ($r.Input -replace '\|', '/'); $exp = ($r.Expected -replace '\|', '/'); $act = ($r.Actual -replace '\|', '/')
  [void]$sb.AppendLine("| $i | $($r.Name) | $inp | $exp | $act | $tag |")
  $i++
}
[void]$sb.AppendLine("")
[void]$sb.AppendLine("## Notes")
[void]$sb.AppendLine("- MI percent = a dimension's share of total MI Likert points; a single spiked")
[void]$sb.AppendLine("  dimension (16 pts vs 8 baseline) reads ~20% and ranks #1, as expected.")
[void]$sb.AppendLine("- Quotients are normalized so the strongest reads 100%. A spike on either MI")
[void]$sb.AppendLine("  dimension feeding a quotient drives that quotient to the top (IQ=LOGICAL+VERBAL,")
[void]$sb.AppendLine("  EQ=INTERPERSONAL+INTRAPERSONAL, AQ=NATURALIST+KINESTHETIC).")
[void]$sb.AppendLine("- Career ranking follows the driving dimensions in ``career_suggestions``.")
[void]$sb.AppendLine("")
[void]$sb.AppendLine("## Finding that led to a formula change")
[void]$sb.AppendLine("- **Career scale mismatch (fixed).** The first run failed one case: a KINESTHETIC")
[void]$sb.AppendLine("  (MI) spike still ranked *Engineering* above *Sports/Performing Arts*. Root cause:")
[void]$sb.AppendLine("  career scoring averaged MI shares (~10-20 each) with RIASEC scores mapped to")
[void]$sb.AppendLine("  0-100, so at baseline any RIASEC-driven field outweighed an MI spike. Fixed by")
[void]$sb.AppendLine("  scoring each driving dimension as its **prominence within its own system**")
[void]$sb.AppendLine("  (value / that system's average; 1.0 = average), putting MI and RIASEC on equal")
[void]$sb.AppendLine("  footing. After the change all 18 fixtures pass and MI spikes correctly steer")
[void]$sb.AppendLine("  their careers (KINESTHETIC -> Performing Arts/Sports, LOGICAL -> Finance/Data).")

$docPath = Join-Path $repoRoot "docs\psychometric-validation.md"
$sb.ToString() | Set-Content -Encoding utf8 $docPath
Write-Host ""
Write-Host "==== $pass / $total passed ===="
Write-Host "Wrote $docPath"
if ($pass -ne $total) { exit 1 }
