# Seeds the three psychometric question banks through the real API (idempotent).
# Creates a demo school "psychobank" and a student "psychobankstudent" under it,
# one category + quiz per grade band, and all tagged Likert questions.
# Emits seeded-index.json (quizId + quesIds per dimension) for the validator.
#
# Usage:  powershell -File seed-banks.ps1  [-BaseUrl http://localhost:8081] [-Pass seedpass123]
param(
  [string]$BaseUrl = "http://localhost:8081",
  [string]$Pass = "seedpass123"
)
$ErrorActionPreference = "Stop"
$here = Split-Path -Parent $MyInvocation.MyCommand.Path

# Every item uses the same 4-point word-anchored agreement scale.
$OPT = @{ option1 = "Strongly disagree"; option2 = "Disagree"; option3 = "Agree"; option4 = "Strongly agree" }

function ApiLogin($u, $p) {
  Invoke-RestMethod "$BaseUrl/api/login" -Method Post -ContentType "application/json" `
    -Body (@{ username = $u; password = $p } | ConvertTo-Json)
}
function Hdr($tok) { @{ Authorization = "Bearer $tok" } }

# --- demo school (idempotent) ---
try { $school = ApiLogin "psychobank" $Pass }
catch {
  Invoke-RestMethod "$BaseUrl/api/register/school" -Method Post -ContentType "application/json" `
    -Body (@{ firstName = "Psycho"; lastName = "Bank"; username = "psychobank"; password = $Pass; phoneNumber = "9000000001" } | ConvertTo-Json) | Out-Null
  $school = ApiLogin "psychobank" $Pass
}
$sh = Hdr $school.jwtToken
$teacherId = $school.user.userId
Write-Host "School psychobank ready (teacherId=$teacherId)"

# --- demo student under this school (idempotent) ---
try { ApiLogin "psychobankstudent" $Pass | Out-Null }
catch {
  Invoke-RestMethod "$BaseUrl/api/register" -Method Post -ContentType "application/json" `
    -Body (@{ firstName = "Bank"; lastName = "Student"; username = "psychobankstudent"; password = $Pass; phoneNumber = "9000000002"; teacherId = $teacherId } | ConvertTo-Json) | Out-Null
}
Write-Host "Student psychobankstudent ready"

$existingCats = Invoke-RestMethod "$BaseUrl/api/category/" -Headers $sh
$existingQuizzes = Invoke-RestMethod "$BaseUrl/api/quiz/" -Headers $sh
$index = @{}

foreach ($file in @("class6-8.json", "class9-10.json", "class11-12.json")) {
  $bank = Get-Content (Join-Path $here $file) -Raw -Encoding UTF8 | ConvertFrom-Json
  $catTitle = "Psychometric " + $bank.band

  # category (reuse if present)
  $cat = $existingCats | Where-Object { $_.title -eq $catTitle } | Select-Object -First 1
  if (-not $cat) {
    $cat = Invoke-RestMethod "$BaseUrl/api/category/" -Method Post -ContentType "application/json" -Headers $sh `
      -Body (@{ title = $catTitle; description = $bank.band + " psychometric bank" } | ConvertTo-Json)
  }

  # quiz (reuse if present)
  $quiz = $existingQuizzes | Where-Object { $_.title -eq $bank.quizTitle } | Select-Object -First 1
  $questionsByDim = @{}

  if ($quiz) {
    Write-Host "$($bank.quizTitle): already exists (quizId=$($quiz.quizId)), reading questions"
    $qs = Invoke-RestMethod "$BaseUrl/api/question/?quizId=$($quiz.quizId)" -Headers $sh
    foreach ($q in $qs) {
      if (-not $questionsByDim.ContainsKey($q.dimension)) { $questionsByDim[$q.dimension] = @() }
      $questionsByDim[$q.dimension] += $q.quesId
    }
  }
  else {
    $quiz = Invoke-RestMethod "$BaseUrl/api/quiz/" -Method Post -ContentType "application/json" -Headers $sh `
      -Body (@{ title = $bank.quizTitle; description = $bank.quizDescription; isActive = $true;
                category = @{ catId = $cat.catId; title = $cat.title; description = $cat.description } } | ConvertTo-Json)
    Write-Host "$($bank.quizTitle): created (quizId=$($quiz.quizId)), posting $($bank.items.Count) questions"
    foreach ($item in $bank.items) {
      $body = @{ content = $item.content; option1 = $OPT.option1; option2 = $OPT.option2;
                 option3 = $OPT.option3; option4 = $OPT.option4; answer = "option4";
                 dimension = $item.dimension; quiz = @{ quizId = $quiz.quizId } } | ConvertTo-Json
      $q = Invoke-RestMethod "$BaseUrl/api/question/" -Method Post -ContentType "application/json" -Headers $sh -Body $body
      if (-not $questionsByDim.ContainsKey($item.dimension)) { $questionsByDim[$item.dimension] = @() }
      $questionsByDim[$item.dimension] += $q.quesId
    }
  }

  $index[$bank.band] = @{ quizId = $quiz.quizId; questions = $questionsByDim }
}

$index | ConvertTo-Json -Depth 6 | Set-Content -Encoding utf8 (Join-Path $here "seeded-index.json")
Write-Host "Wrote seeded-index.json"
