import React, { useEffect, useRef, useState, useCallback } from "react";
import { Link, useNavigate } from "react-router-dom";
import { homePathForRoles } from "../components/ProtectedRoute";
import styles from "./LandingPage.module.css";

/*
 * Public marketing homepage at "/" for The Mentalist.
 * Presentational only: booking submits straight to WhatsApp (no backend
 * endpoint), and the quiz/tabs/testimonial slider are local UI state.
 * Already-authenticated visitors are bounced to their dashboard (same rule
 * the Login page uses). Login/Register live in the global <Header/>, not here.
 */

const EXPERTISE = [
  { icon: "fa-brain", text: "Mental Health Coaching" },
  { icon: "fa-comments", text: "Student Counseling" },
  { icon: "fa-child-reaching", text: "Adolescence Development" },
  { icon: "fa-bolt", text: "Academic Stress Management" },
  { icon: "fa-heart", text: "Emotional Well-being" },
  { icon: "fa-shield-halved", text: "Confidence Building" },
  { icon: "fa-route", text: "Career Counseling & Guidance" },
  { icon: "fa-arrow-up-right-dots", text: "Goal Setting & Growth" },
];

const ASSESSMENTS = [
  {
    id: "student-intelligence",
    icon: "fa-graduation-cap",
    name: "Student Intelligence",
    tagline: "Stream Selection / Career Path",
    title: "Student Intelligence Assessment",
    tag: "Most Popular",
    tagStyle: { background: "rgba(2, 132, 199, 0.15)" },
    desc: "A specialized psychometric assessment that helps students identify the most suitable career path and subject stream after 10th & 12th based on their interests, aptitude, personality traits, and inherent abilities.",
    features: [
      "Maps stream selection for Grade 10 and 12",
      "Understands personal interest & aptitude profiles",
      "Delivers scientific, data-backed insights",
      "Facilitates informed future choices",
    ],
  },
  {
    id: "iq-plus",
    icon: "fa-brain",
    name: "IQ Plus Assessment",
    tagline: "Cognitive Abilities & Learning",
    title: "IQ Plus Assessment",
    tag: "Cognitive",
    tagStyle: { background: "rgba(13, 148, 136, 0.15)" },
    desc: "A scientifically designed psychometric evaluation that identifies a student's cognitive abilities, learning potential, working memory strengths, and core areas for improvements.",
    features: [
      "Evaluates logical and mathematical reasoning",
      "Identifies spatial and visual memory capacity",
      "Provides strategies to improve academic focus",
      "Assesses overall cognitive learning potential",
    ],
  },
  {
    id: "mental-skills",
    icon: "fa-heart-circle-check",
    name: "Mental Skills",
    tagline: "Stress, Anxiety & Resilience",
    title: "Mental Skills Assessment",
    tag: "Wellness Focus",
    tagStyle: { background: "rgba(239, 68, 68, 0.15)", color: "#ef4444", borderColor: "rgba(239, 68, 68, 0.3)" },
    desc: "A comprehensive mental health and resilience screening designed to evaluate a student's stress levels, emotional well-being, anxiety management capabilities, and mental resilience.",
    features: [
      "Measures academic stress & peer pressure indicators",
      "Screens anxiety and confidence vulnerabilities",
      "Evaluates emotional regulation & mindfulness",
      "Guides healthy emotional & social development",
    ],
  },
  {
    id: "overseas",
    icon: "fa-plane-departure",
    name: "Overseas Assessment",
    tagline: "Study Abroad & Adaptability",
    title: "Overseas Assessment",
    tag: "Study Abroad",
    tagStyle: { background: "rgba(168, 85, 247, 0.15)", color: "#a855f7", borderColor: "rgba(168, 85, 247, 0.3)" },
    desc: "An aptitude and adaptability evaluation designed to understand a student's personality readiness, cultural adaptability, and suitability for pursuing higher education in foreign universities.",
    features: [
      "Assesses psychological suitability for study abroad",
      "Measures independence & emotional maturity",
      "Assists in choosing global courses & countries",
      "Supports better decision-making for overseas living",
    ],
  },
  {
    id: "brain-benchmark",
    icon: "fa-chart-line",
    name: "Brain Benchmark",
    tagline: "Strengths & Leadership Profiling",
    title: "Brain Benchmark Assessment",
    tag: "Leadership",
    tagStyle: { background: "rgba(234, 179, 8, 0.15)", color: "#eab308", borderColor: "rgba(234, 179, 8, 0.3)" },
    desc: "A comprehensive skills benchmark for students, young professionals, and aspiring entrepreneurs to discover their key competencies, leadership potential, entrepreneurial mindsets, and ideal career suitability.",
    features: [
      "Profiles leadership qualities & team skills",
      "Identifies key entrepreneurial competencies",
      "Helps make informed career path shifts",
      "Maps personal strengths to industry roles",
    ],
  },
];

const QUIZ_QUESTIONS = [
  {
    category: "Profile Check",
    key: "profile",
    title: "What is your current academic or professional status?",
    options: [
      { text: "School Student (Grade 8 to 12)", scoreKey: "student" },
      { text: "College / University Student", scoreKey: "college" },
      { text: "Working Professional or Entrepreneur", scoreKey: "professional" },
      { text: "Parent seeking guidance for my child", scoreKey: "parent" },
    ],
  },
  {
    category: "Core Objective",
    key: "objective",
    title: "What is the primary milestone or challenge you face?",
    options: [
      { text: "Choosing the correct stream/career after 10th or 12th", scoreKey: "career" },
      { text: "Testing logical/numerical learning potential", scoreKey: "iq" },
      { text: "Managing anxiety, stress, or emotional resilience", scoreKey: "wellness" },
      { text: "Planning for overseas education & adaptability abroad", scoreKey: "overseas" },
      { text: "Finding leadership strengths, business skills, or pivots", scoreKey: "benchmark" },
    ],
  },
  {
    category: "Format Preference",
    key: "format",
    title: "Which counseling format do you feel most comfortable with?",
    options: [
      { text: "Scientific testing reports & direct data profiling", scoreKey: "reports" },
      { text: "One-on-one personal coaching & mental support", scoreKey: "counseling" },
      { text: "Both testing and active, ongoing personal guidance", scoreKey: "both" },
    ],
  },
];

const RECOMMENDATION_MAP = {
  career: {
    name: "Student Intelligence Assessment",
    desc: "Perfect for defining the ideal academic subject stream or career roadmap post 10th/12th based on interests, abilities, and personality.",
  },
  iq: {
    name: "IQ Plus Assessment",
    desc: "Ideal for profiling basic cognitive functions, working memory potential, and logical reasoning to structure custom academic roadmaps.",
  },
  wellness: {
    name: "Mental Skills Assessment",
    desc: "Specially designed to assess mental stress, anxiety indicators, study pressure, and build emotional resilience and confidence.",
  },
  overseas: {
    name: "Overseas Assessment",
    desc: "Tailored to evaluate independent adaptability, personal readiness, and suitable countries/courses for studying abroad.",
  },
  benchmark: {
    name: "Brain Benchmark Assessment",
    desc: "Best for professionals, graduates, and entrepreneurs to outline leadership abilities, core strengths, and career pivot suitabilities.",
  },
};

const TESTIMONIALS = [
  {
    quote: "The Student Intelligence Assessment was a game-changer for my daughter. She was extremely confused between Commerce and Humanities, but the psychometric data showed her clear strength in economic analysis and management. We saved a lot of trial-and-error time!",
    initial: "A",
    name: "Anju Sharma",
    role: "Parent, Jalandhar",
  },
  {
    quote: "I was facing severe anxiety during my Board exams. The counseling sessions helped me build stress-coping strategies, structure my routine, and approach study in a calm way. My percentage improved and, more importantly, my mental health did too.",
    initial: "R",
    name: "Rohan Malhotra",
    role: "Grade 12 Student",
  },
  {
    quote: "After completing 6 years of work experience, I wanted to change paths. The Brain Benchmark assessment profiled my leadership traits and suggested matching roles. The personal coaching sessions gave me the clarity and confidence to make a successful career pivot.",
    initial: "S",
    name: "Sukhvinder Singh",
    role: "Young Professional",
  },
];

const SERVICE_OPTIONS = [
  "Student Intelligence Assessment",
  "IQ Plus Assessment",
  "Mental Skills Assessment",
  "Overseas Assessment",
  "Brain Benchmark Assessment",
  "Mental Health / Wellness Counseling",
  "General Consultation & Goal Coaching",
  "School / Institution Partnership Inquiry",
];

const WHATSAPP_NUMBER = "917696029559";
const PHONE_DISPLAY = "+91 7696029559";

// A DOM class list wired to a shared IntersectionObserver reveals each
// .reveal element as it scrolls into view; the class itself is scoped by
// LandingPage.module.css.
function useRevealOnScroll(containerRef) {
  useEffect(() => {
    const root = containerRef.current;
    if (!root) return undefined;
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add(styles.revealActive);
          }
        });
      },
      { threshold: 0.1, rootMargin: "0px 0px -40px 0px" }
    );
    // CSS Modules hashes can contain "+" and "/", which are combinators in a
    // selector — CSS.escape keeps the generated name a single class token.
    root.querySelectorAll(`.${CSS.escape(styles.reveal)}`).forEach((el) => observer.observe(el));
    return () => observer.disconnect();
  }, [containerRef]);
}

function useAnimatedStats(statsRef) {
  const [animated, setAnimated] = useState(false);
  const [values, setValues] = useState(null);

  useEffect(() => {
    const el = statsRef.current;
    if (!el) return undefined;
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting && !animated) {
            setAnimated(true);
            const targets = [
              { target: 6, suffix: "+ Yrs" },
              { target: 2500, suffix: "+" },
              { target: 100, suffix: "%" },
            ];
            const duration = 1800;
            let start = null;
            const step = (timestamp) => {
              if (!start) start = timestamp;
              const progress = Math.min((timestamp - start) / duration, 1);
              setValues(
                targets.map((t) => Math.floor(progress * t.target).toLocaleString() + t.suffix)
              );
              if (progress < 1) window.requestAnimationFrame(step);
            };
            window.requestAnimationFrame(step);
          }
        });
      },
      { threshold: 0.5 }
    );
    observer.observe(el);
    return () => observer.disconnect();
  }, [statsRef, animated]);

  return values || ["0+ Yrs", "0+", "0%"];
}

const LandingPage = () => {
  const navigate = useNavigate();
  const pageRef = useRef(null);
  const statsRef = useRef(null);
  const trackRef = useRef(null);

  const [theme, setTheme] = useState(() => localStorage.getItem("mtTheme") || "dark");
  const [scrolled, setScrolled] = useState(false);
  const [activeAssessment, setActiveAssessment] = useState(ASSESSMENTS[0].id);

  const [quizStep, setQuizStep] = useState(0);
  const [quizAnswers, setQuizAnswers] = useState({});

  const [testimonialIndex, setTestimonialIndex] = useState(0);
  const [cardsPerView, setCardsPerView] = useState(3);

  const [modalOpen, setModalOpen] = useState(false);
  const [confirmed, setConfirmed] = useState(false);
  const [form, setForm] = useState({ name: "", phone: "", profile: "", service: "", date: "" });
  const [whatsappUrl, setWhatsappUrl] = useState("");

  useRevealOnScroll(pageRef);
  const statValues = useAnimatedStats(statsRef);

  // Authenticated visitors don't need the marketing page — send them home.
  useEffect(() => {
    if (!localStorage.getItem("user")) return;
    try {
      const user = JSON.parse(localStorage.getItem("user"));
      const roles = user && user.roles ? user.roles.map((r) => r.roleName) : [];
      navigate(homePathForRoles(roles), { replace: true });
    } catch (e) {
      /* not logged in with a valid user blob; stay on the landing page */
    }
  }, [navigate]);

  useEffect(() => {
    document.documentElement.setAttribute("data-theme", theme);
    localStorage.setItem("mtTheme", theme);
  }, [theme]);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 40);
    window.addEventListener("scroll", onScroll);
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  const updateCardsPerView = useCallback(() => {
    if (window.innerWidth <= 768) setCardsPerView(1);
    else if (window.innerWidth <= 992) setCardsPerView(2);
    else setCardsPerView(3);
  }, []);

  useEffect(() => {
    updateCardsPerView();
    const onResize = () => {
      setTestimonialIndex(0);
      updateCardsPerView();
    };
    window.addEventListener("resize", onResize);
    return () => window.removeEventListener("resize", onResize);
  }, [updateCardsPerView]);

  const openBookingModal = (preselectedService) => {
    setConfirmed(false);
    setForm((f) => ({ ...f, service: preselectedService || f.service }));
    setModalOpen(true);
  };
  const closeBookingModal = () => setModalOpen(false);

  const handleBookingSubmit = (e) => {
    e.preventDefault();
    const { name, phone, profile, service, date } = form;
    const message = `Hello The Mentalist! I would like to book a consultation session.\nDetails:\n- Name: ${name}\n- Phone: ${phone}\n- Profile: ${profile}\n- Service: ${service}\n- Preferred Date: ${date}`;
    setWhatsappUrl(`https://wa.me/${WHATSAPP_NUMBER}?text=${encodeURIComponent(message)}`);
    setConfirmed(true);
  };

  const currentQuizQuestion = QUIZ_QUESTIONS[quizStep];
  const quizProgress = (quizStep / QUIZ_QUESTIONS.length) * 100;
  const recommendedTest = RECOMMENDATION_MAP[quizAnswers.objective] || RECOMMENDATION_MAP.career;

  const handleQuizSelection = (questionKey, scoreKey) => {
    setQuizAnswers((prev) => ({ ...prev, [questionKey]: scoreKey }));
    setTimeout(() => setQuizStep((s) => s + 1), 200);
  };
  const resetQuiz = () => {
    setQuizStep(0);
    setQuizAnswers({});
  };

  const minBookingDate = new Date().toISOString().split("T")[0];
  const todayYear = new Date().getFullYear();

  const shiftTestimonials = (dir) => {
    setTestimonialIndex((idx) => {
      const max = TESTIMONIALS.length - cardsPerView;
      const next = idx + dir;
      if (next < 0 || next > max) return idx;
      return next;
    });
  };
  const cardWidth = trackRef.current?.querySelector(`.${styles.testimonialCard}`)?.getBoundingClientRect().width || 0;
  const trackShift = testimonialIndex * (cardWidth + 32);

  return (
    <div className={styles.page} ref={pageRef}>
      {/* Floating Glass Navigation Bar */}
      <nav className={`${styles.navbar} ${scrolled ? styles.navbarScrolled : ""}`}>
        <div className={`${styles.container} ${styles.navContainer}`}>
          <a href="#hero" className={styles.navBrand}>
            <div className={styles.brandIcon}>
              <i className="fa-solid fa-brain" />
            </div>
            <span>
              THE <span className={styles.textGradient}>MENTALIST</span>
            </span>
          </a>

          <ul className={styles.navLinks}>
            <li><a href="#hero" className={styles.navLink}>Overview</a></li>
            <li><a href="#about" className={styles.navLink}>About Us</a></li>
            <li><a href="#services" className={styles.navLink}>Services</a></li>
            <li><a href="#quiz-section" className={styles.navLink}>Find Assessment</a></li>
            <li><a href="#assessments" className={styles.navLink}>Assessments</a></li>
          </ul>

          <div className={styles.navActions}>
            <button
              type="button"
              className={styles.themeToggle}
              title="Toggle Theme"
              onClick={() => setTheme((t) => (t === "dark" ? "light" : "dark"))}
            >
              <i className={`fa-solid ${theme === "light" ? "fa-sun" : "fa-moon"}`} />
            </button>
            <Link to="/login" className={styles.btnPrimary}>
              <span>Login</span>
              <i className="fa-solid fa-arrow-right" />
            </Link>
          </div>
        </div>
      </nav>

      {/* Hero Section */}
      <section className={styles.hero} id="hero">
        <div className={styles.heroGlow} />
        <div className={`${styles.container} ${styles.heroGrid}`}>
          <div className={`${styles.heroContent} ${styles.reveal}`}>
            <div className={styles.badge}>
              <span className={styles.badgeDot} />
              <span>Guiding Minds. Shaping Futures.</span>
            </div>

            <h1 className={styles.heroTitle}>
              Empowering Minds.<br />
              <span className={styles.textGradient}>Building Futures.</span>
            </h1>

            <p className={styles.heroDescription}>
              Expert life coaching, mental health coaching, stream selection, and career
              counseling. We help students and young professionals build confidence, manage
              stress, and choose the right career path using science-based assessments.
            </p>

            <div className={styles.heroCta}>
              <button type="button" className={styles.btnPrimary} onClick={() => openBookingModal()}>
                <i className="fa-solid fa-calendar-check" />
                <span>Schedule Session</span>
              </button>
              <a href="#about" className={styles.btnSecondary}>
                <span>Learn More</span>
                <i className="fa-solid fa-chevron-down" />
              </a>
            </div>

            <div className={styles.heroStats} ref={statsRef}>
              <div>
                <div className={`${styles.statNumber} ${styles.textGradient}`}>{statValues[0]}</div>
                <div className={styles.statLabel}>Professional Exp.</div>
              </div>
              <div>
                <div className={`${styles.statNumber} ${styles.textEmeraldGradient}`}>{statValues[1]}</div>
                <div className={styles.statLabel}>Students Guided</div>
              </div>
              <div>
                <div className={`${styles.statNumber} ${styles.textGradient}`}>{statValues[2]}</div>
                <div className={styles.statLabel}>Personalized Care</div>
              </div>
            </div>
          </div>

          <div className={`${styles.heroVisual} ${styles.reveal}`}>
            <div className={styles.heroImageCard}>
              <img src="/mentalist/hero_counseling.jpg" alt="Coaching session with The Mentalist" />
            </div>
            <div className={styles.heroFloatingPill}>
              <div className={styles.pillIcon}>
                <i className="fa-solid fa-graduation-cap" />
              </div>
              <div>
                <div style={{ fontWeight: 700, fontSize: "0.95rem" }}>Holistic Growth Focus</div>
                <div style={{ fontSize: "0.8rem", color: "var(--mt-text-secondary)" }}>Empowering the New Gen</div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* About Us & Vision/Mission Section */}
      <section className={`${styles.section} ${styles.sectionAlt}`} id="about">
        <div className={styles.container}>
          <div className={`${styles.sectionHeader} ${styles.reveal}`}>
            <div className={styles.badge}>Who We Are</div>
            <h2 className={styles.sectionTitle}>Discover Your Strengths. Shape Your Future.</h2>
            <p className={styles.sectionSubtitle}>
              We guide students and families to overcome emotional hurdles, improve
              self-awareness, and find their perfect academic and career pathways.
            </p>
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(320px, 1fr))", gap: 24, marginBottom: 56 }}>
            <div className={`${styles.bentoCard} ${styles.reveal}`} style={{ padding: 30 }}>
              <div className={styles.bentoCardContent}>
                <span className={styles.bentoTag} style={{ color: "var(--mt-accent-blue)" }}>
                  <i className="fa-solid fa-eye" /> Our Vision
                </span>
                <h3 style={{ fontSize: "1.5rem", marginTop: 8 }}>&quot;Strong Minds Create Successful Futures&quot;</h3>
                <p className={styles.bentoDescription} style={{ marginTop: 10 }}>
                  To develop confident, capable, and responsible individuals through quality
                  guidance, personalized support, and holistic growth, empowering them to make a
                  positive impact on the world.
                </p>
              </div>
            </div>

            <div className={`${styles.bentoCard} ${styles.reveal}`} style={{ padding: 30 }}>
              <div className={styles.bentoCardContent}>
                <span className={styles.bentoTag} style={{ color: "var(--mt-accent-cyan)" }}>
                  <i className="fa-solid fa-bullseye" /> Our Mission
                </span>
                <h3 style={{ fontSize: "1.5rem", marginTop: 8 }}>Empower through Resilience</h3>
                <p className={styles.bentoDescription} style={{ marginTop: 10 }}>
                  To equip students and young professionals with the emotional resilience,
                  clarity, confidence, and scientific insights needed to make informed life and
                  career choices.
                </p>
              </div>
            </div>
          </div>

          <div className={styles.reveal}>
            <h3 style={{ fontSize: "1.8rem", textAlign: "center", marginBottom: 24 }}>Areas of Expertise</h3>
            <div className={styles.expertiseGrid}>
              {EXPERTISE.map((e) => (
                <div className={styles.expertiseCard} key={e.text}>
                  <i className={`fa-solid ${e.icon}`} />
                  <span>{e.text}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* Bento Grid Services Section */}
      <section className={styles.section} id="services">
        <div className={styles.container}>
          <div className={`${styles.sectionHeader} ${styles.reveal}`}>
            <div className={styles.badge}>Our Programs</div>
            <h2 className={styles.sectionTitle}>Holistic Development Under One Roof</h2>
            <p className={styles.sectionSubtitle}>
              From school wellness initiatives to targeted career assessments, we support every
              phase of a student&apos;s journey.
            </p>
          </div>

          <div className={styles.bentoGrid}>
            <div className={`${styles.bentoCard} ${styles.bentoCol8} ${styles.reveal}`}>
              <div className={styles.bentoCardContent}>
                <span className={styles.bentoTag}>Academic Partnership</span>
                <h3 className={`${styles.bentoTitle} ${styles.textGradient}`}>
                  School Mental Wellness & Career Guidance
                </h3>
                <p className={styles.bentoDescription}>
                  We collaborate with schools and educational institutions to implement
                  structured mental health support, emotional resilience building, and career
                  readiness programs. Supporting emotional health, self-discovery, and academic
                  transition points seamlessly.
                </p>
                <div style={{ display: "flex", gap: 12, marginTop: 16, flexWrap: "wrap" }}>
                  <span className={`${styles.badge} ${styles.softBadge}`}>Student Well-being Workshops</span>
                  <span className={`${styles.badge} ${styles.softBadge}`}>Parent-Teacher Alignment</span>
                  <span className={`${styles.badge} ${styles.softBadge}`}>Teacher Empowerment Session</span>
                </div>
              </div>
            </div>

            <div className={`${styles.bentoCard} ${styles.bentoCol4} ${styles.reveal}`}>
              <div className={styles.bentoCardContent}>
                <span className={styles.bentoTag}>Aptitude & Choice</span>
                <h3 className={styles.bentoTitle}>Stream & Career Selection</h3>
                <p className={styles.bentoDescription}>
                  Step-by-step assistance in choosing the correct subject streams after 10th and
                  12th grades based on personality type, aptitude, and long-term professional
                  aspirations.
                </p>
                <div style={{ marginTop: 20, color: "var(--mt-accent-blue)", fontWeight: 600, fontSize: "0.95rem", display: "flex", alignItems: "center", gap: 8 }}>
                  <i className="fa-solid fa-graduation-cap" /> Higher Education Planning
                </div>
              </div>
            </div>

            <div className={`${styles.bentoCard} ${styles.bentoCol4} ${styles.reveal}`}>
              <div className={styles.bentoCardContent}>
                <span className={styles.bentoTag}>Mental Health</span>
                <h3 className={styles.bentoTitle}>Emotional & Behavioral Counseling</h3>
                <p className={styles.bentoDescription}>
                  Safe, confidential spaces for students and adolescents to navigate anxiety,
                  stress, emotional blockages, self-doubt, and behavioral challenges.
                </p>
                <div style={{ marginTop: 20, color: "var(--mt-accent-cyan)", fontWeight: 600, fontSize: "0.95rem", display: "flex", alignItems: "center", gap: 8 }}>
                  <i className="fa-solid fa-heart-pulse" /> Stress Resilience
                </div>
              </div>
            </div>

            {/* Interactive Quiz (Find Your Assessment) */}
            <div className={`${styles.bentoCard} ${styles.bentoCol8} ${styles.reveal}`} id="quiz-section">
              <div className={styles.bentoCardContent}>
                <span className={styles.bentoTag} style={{ color: "var(--mt-accent-emerald, #10b981)" }}>Interactive Finder</span>
                <h3 className={`${styles.bentoTitle} ${styles.textEmeraldGradient}`}>Find the Assessment You Need</h3>
                <p className={styles.bentoDescription}>
                  Unsure which psychometric evaluation matches your current stage? Take our
                  quick 3-click self-assessment to discover the perfect tool for your goals.
                </p>

                <div className={styles.quizContainer}>
                  <div className={styles.quizProgressBar} style={{ width: `${quizStep >= QUIZ_QUESTIONS.length ? 100 : quizProgress}%` }} />

                  {quizStep < QUIZ_QUESTIONS.length ? (
                    <>
                      <div className={styles.quizHeader}>
                        <span style={{ fontWeight: 700, fontSize: "0.9rem", color: "var(--mt-accent-cyan)" }}>
                          Question {quizStep + 1} of {QUIZ_QUESTIONS.length}
                        </span>
                        <span className={styles.badge}>{currentQuizQuestion.category}</span>
                      </div>
                      <div className={styles.quizQuestionBox}>
                        <h4 className={styles.quizQuestionTitle}>{currentQuizQuestion.title}</h4>
                        <div className={styles.quizOptionsGrid}>
                          {currentQuizQuestion.options.map((opt, idx) => (
                            <button
                              type="button"
                              key={opt.text}
                              className={styles.quizOptionBtn}
                              onClick={() => handleQuizSelection(currentQuizQuestion.key, opt.scoreKey)}
                            >
                              <div className={styles.optionIndicator}>{String.fromCharCode(65 + idx)}</div>
                              <span>{opt.text}</span>
                            </button>
                          ))}
                        </div>
                      </div>
                    </>
                  ) : (
                    <>
                      <div className={styles.quizHeader}>
                        <span style={{ fontWeight: 700, fontSize: "0.9rem", color: "var(--mt-accent-cyan)" }}>Completed</span>
                        <span className={styles.badge} style={{ background: "rgba(16, 185, 129, 0.15)", color: "var(--mt-accent-emerald, #10b981)" }}>
                          Result
                        </span>
                      </div>
                      <div className={styles.quizQuestionBox}>
                        <div className={styles.quizResultCard}>
                          <span className={styles.quizResultBadge}>Highly Recommended</span>
                          <h4 className={styles.quizResultTitle}>{recommendedTest.name}</h4>
                          <p className={styles.quizResultDesc}>{recommendedTest.desc}</p>
                          <div className={styles.quizActionGroup}>
                            <button type="button" className={styles.btnPrimary} onClick={() => openBookingModal(recommendedTest.name)}>
                              <i className="fa-solid fa-calendar-plus" />
                              <span>Schedule This Assessment</span>
                            </button>
                            <button type="button" className={styles.btnSecondary} onClick={resetQuiz}>
                              <i className="fa-solid fa-rotate-left" />
                              <span>Retake Check</span>
                            </button>
                          </div>
                        </div>
                      </div>
                    </>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Detailed Psychometric Assessments Tabs Section */}
      <section className={styles.assessmentsSection} id="assessments">
        <div className={styles.container}>
          <div className={`${styles.sectionHeader} ${styles.reveal}`}>
            <div className={styles.badge}>Science-Based Tools</div>
            <h2 className={styles.sectionTitle}>Our Psychometric Assessments</h2>
            <p className={styles.sectionSubtitle}>
              We employ scientifically validated profiling methods to map interest, cognitive
              ability, strengths, and resilience.
            </p>
          </div>

          <div className={`${styles.assessmentsTabsContainer} ${styles.reveal}`}>
            <div className={styles.assessmentsTabsNav}>
              {ASSESSMENTS.map((a) => (
                <button
                  type="button"
                  key={a.id}
                  className={`${styles.assessmentTabBtn} ${activeAssessment === a.id ? styles.assessmentTabBtnActive : ""}`}
                  onClick={() => setActiveAssessment(a.id)}
                >
                  <div className={styles.assessmentTabIcon}>
                    <i className={`fa-solid ${a.icon}`} />
                  </div>
                  <div className={styles.assessmentTabInfo}>
                    <span className={styles.assessmentTabName}>{a.name}</span>
                    <span className={styles.assessmentTabTagline}>{a.tagline}</span>
                  </div>
                </button>
              ))}
            </div>

            <div className={styles.assessmentsTabsContent}>
              {ASSESSMENTS.filter((a) => a.id === activeAssessment).map((a) => (
                <div className={styles.assessmentPanel} key={a.id}>
                  <div>
                    <div className={styles.panelHeader}>
                      <h3 className={`${styles.panelTitle} ${styles.textGradient}`}>{a.title}</h3>
                      <span className={styles.badge} style={a.tagStyle}>{a.tag}</span>
                    </div>
                    <p className={styles.panelDesc}>{a.desc}</p>
                    <div className={styles.panelFeatures}>
                      {a.features.map((f) => (
                        <div className={styles.panelFeatureItem} key={f}>
                          <i className="fa-solid fa-circle-check" />
                          <span>{f}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                  <button
                    type="button"
                    className={styles.btnPrimary}
                    style={{ marginTop: 32, alignSelf: "flex-start" }}
                    onClick={() => openBookingModal(a.title)}
                  >
                    <span>Inquire About Assessment</span>
                    <i className="fa-solid fa-chevron-right" />
                  </button>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* Why Mental Health Matters Section */}
      <section className={styles.section} id="why-mental-health">
        <div className={styles.container}>
          <div className={`${styles.sectionHeader} ${styles.reveal}`}>
            <div className={styles.badge}>Mind & Body Alignment</div>
            <h2 className={styles.sectionTitle}>Why Mental Health Matters</h2>
            <p className={styles.sectionSubtitle}>
              Academic scores do not define a student&apos;s complete future. Emotional
              well-being and resilience are the bedrock of success.
            </p>
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "1.1fr 0.9fr", gap: 48, alignItems: "center" }}>
            <div className={styles.reveal} style={{ display: "flex", flexDirection: "column", gap: 24 }}>
              <h3 style={{ fontSize: "1.8rem" }}>
                Emotional resilience leads to academic excellence and life success.
              </h3>
              <p style={{ color: "var(--mt-text-secondary)", fontSize: "1.05rem" }}>
                A student&apos;s potential is unlocked when emotional well-being, self-belief,
                and mental resilience support their efforts. At The Mentalist, we guide students
                to:
              </p>
              <ul className={styles.checklist}>
                <li><i className="fa-solid fa-circle-check" /> Manage stress, anxiety, and performance pressure</li>
                <li><i className="fa-solid fa-circle-check" /> Improve concentration, memory, and cognitive focus</li>
                <li><i className="fa-solid fa-circle-check" /> Build strong self-confidence and self-awareness</li>
                <li><i className="fa-solid fa-circle-check" /> Develop structured positive thinking patterns</li>
                <li><i className="fa-solid fa-circle-check" /> Handle social, familial, and academic pressures</li>
                <li><i className="fa-solid fa-circle-check" /> Strengthen emotional intelligence and life decision-making</li>
              </ul>
            </div>

            <div className={`${styles.reveal} ${styles.heroVisual}`} style={{ position: "relative" }}>
              <div className={styles.heroImageCard} style={{ borderColor: "var(--mt-accent-cyan)" }}>
                <img src="/mentalist/student_wellness.jpg" alt="Student Emotional Well-being Concept" />
              </div>
              <div className={`${styles.heroFloatingPill} ${styles.heroFloatingPillDelay}`} style={{ bottom: -20, left: -20, borderColor: "var(--mt-accent-cyan)" }}>
                <div className={styles.pillIcon} style={{ background: "rgba(13, 148, 136, 0.15)", color: "var(--mt-accent-cyan)" }}>
                  <i className="fa-solid fa-quote-left" />
                </div>
                <div>
                  <div style={{ fontWeight: 700, fontSize: "0.95rem", fontStyle: "italic", maxWidth: 240 }}>
                    &quot;When students understand themselves better, they make better decisions.&quot;
                  </div>
                  <div style={{ fontSize: "0.8rem", color: "var(--mt-text-muted)", marginTop: 4 }}>- Our Philosophy</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Testimonials Carousel Section */}
      <section className={`${styles.section} ${styles.sectionAlt}`} id="testimonials">
        <div className={styles.container}>
          <div className={`${styles.sectionHeader} ${styles.reveal}`}>
            <div className={styles.badge}>Impact Stories</div>
            <h2 className={styles.sectionTitle}>Hear From Parents & Students</h2>
            <p className={styles.sectionSubtitle}>
              Real feedback from some of the 2,500+ lives and families transformed by The
              Mentalist.
            </p>
          </div>

          <div className={`${styles.testimonialsSliderContainer} ${styles.reveal}`}>
            <div className={styles.testimonialsTrack} ref={trackRef} style={{ transform: `translateX(-${trackShift}px)` }}>
              {TESTIMONIALS.map((t) => (
                <div className={styles.testimonialCard} key={t.name}>
                  <div className={styles.testimonialStars}>
                    {[...Array(5)].map((_, i) => <i className="fa-solid fa-star" key={i} />)}
                  </div>
                  <p className={styles.testimonialQuote}>&quot;{t.quote}&quot;</p>
                  <div className={styles.testimonialAuthor}>
                    <div className={styles.testimonialAvatar}>{t.initial}</div>
                    <div className={styles.authorMeta}>
                      <span className={styles.authorName}>{t.name}</span>
                      <span className={styles.authorRole}>{t.role}</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>

            <div className={styles.testimonialsNav}>
              <button
                type="button"
                className={styles.testimonialsNavBtn}
                aria-label="Previous testimonials"
                disabled={testimonialIndex === 0}
                onClick={() => shiftTestimonials(-1)}
              >
                <i className="fa-solid fa-chevron-left" />
              </button>
              <button
                type="button"
                className={styles.testimonialsNavBtn}
                aria-label="Next testimonials"
                disabled={testimonialIndex >= TESTIMONIALS.length - cardsPerView}
                onClick={() => shiftTestimonials(1)}
              >
                <i className="fa-solid fa-chevron-right" />
              </button>
            </div>
          </div>
        </div>
      </section>

      {/* Appointment Booking Modal Wizard */}
      <div className={`${styles.modalBackdrop} ${modalOpen ? styles.modalBackdropActive : ""}`}>
        <div className={styles.modalCard}>
          <button type="button" className={styles.modalClose} onClick={closeBookingModal} aria-label="Close">
            <i className="fa-solid fa-xmark" />
          </button>

          {!confirmed ? (
            <div>
              <div style={{ marginBottom: 24 }}>
                <div className={styles.badge} style={{ marginBottom: 8 }}>Step 1 of 2</div>
                <h3 style={{ fontSize: "1.75rem" }}>Connect with the Mentalist</h3>
                <p style={{ color: "var(--mt-text-secondary)", fontSize: "0.95rem" }}>
                  Fill out your details to schedule a counseling session or assessment.
                </p>
              </div>

              <form onSubmit={handleBookingSubmit}>
                <div className={styles.formGroup}>
                  <label className={styles.formLabel} htmlFor="patientName">Full Name</label>
                  <input
                    type="text"
                    className={styles.formInput}
                    id="patientName"
                    placeholder="Enter your full name"
                    required
                    value={form.name}
                    onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
                  />
                </div>

                <div className={styles.formGroup}>
                  <label className={styles.formLabel} htmlFor="patientPhone">Phone / WhatsApp Number</label>
                  <input
                    type="tel"
                    className={styles.formInput}
                    id="patientPhone"
                    placeholder="+91 98765 43210"
                    required
                    value={form.phone}
                    onChange={(e) => setForm((f) => ({ ...f, phone: e.target.value }))}
                  />
                </div>

                <div className={styles.formGroup}>
                  <label className={styles.formLabel} htmlFor="patientProfile">Your Current Profile</label>
                  <select
                    className={styles.formSelect}
                    id="patientProfile"
                    required
                    value={form.profile}
                    onChange={(e) => setForm((f) => ({ ...f, profile: e.target.value }))}
                  >
                    <option value="">Select Profile</option>
                    <option value="Student - Grade 8-10">Student - Grade 8-10</option>
                    <option value="Student - Grade 11-12">Student - Grade 11-12</option>
                    <option value="College / University Student">College / University Student</option>
                    <option value="Working Professional">Working Professional</option>
                    <option value="Parent inquiring for child">Parent inquiring for child</option>
                    <option value="School / Institution Representative">School / Institution Representative</option>
                  </select>
                </div>

                <div className={styles.formGroup}>
                  <label className={styles.formLabel} htmlFor="patientService">Service Needed</label>
                  <select
                    className={styles.formSelect}
                    id="patientService"
                    required
                    value={form.service}
                    onChange={(e) => setForm((f) => ({ ...f, service: e.target.value }))}
                  >
                    <option value="">Select Service</option>
                    {SERVICE_OPTIONS.map((s) => <option value={s} key={s}>{s}</option>)}
                  </select>
                </div>

                <div className={styles.formGroup}>
                  <label className={styles.formLabel} htmlFor="patientDate">Preferred Date</label>
                  <input
                    type="date"
                    className={styles.formInput}
                    id="patientDate"
                    required
                    min={minBookingDate}
                    value={form.date}
                    onChange={(e) => setForm((f) => ({ ...f, date: e.target.value }))}
                  />
                </div>

                <button type="submit" className={styles.btnPrimary} style={{ width: "100%", marginTop: 12 }}>
                  <span>Submit details</span>
                  <i className="fa-solid fa-check" />
                </button>
              </form>
            </div>
          ) : (
            <div style={{ textAlign: "center", padding: "20px 0" }}>
              <div style={{ width: 70, height: 70, borderRadius: "50%", background: "rgba(13, 148, 136, 0.2)", color: "var(--mt-accent-cyan)", display: "flex", alignItems: "center", justifyContent: "center", fontSize: "2.2rem", margin: "0 auto 20px" }}>
                <i className="fa-solid fa-circle-check" />
              </div>
              <h3 style={{ fontSize: "1.8rem", marginBottom: 10 }}>Details Saved!</h3>
              <p style={{ color: "var(--mt-text-secondary)", maxWidth: 440, margin: "0 auto 24px" }}>
                Thank you, {form.name}! Your consultation request for &quot;{form.service}&quot; on{" "}
                {form.date} has been logged. Send a WhatsApp message to instantly connect with us.
              </p>

              <div style={{ display: "flex", flexDirection: "column", gap: 12, alignItems: "center" }}>
                <a
                  href={whatsappUrl}
                  target="_blank"
                  rel="noreferrer"
                  className={styles.btnPrimary}
                  style={{ background: "#25d366", boxShadow: "0 4px 20px rgba(37, 211, 102, 0.4)", border: "none", width: "100%", justifyContent: "center" }}
                >
                  <i className="fa-brands fa-whatsapp" />
                  <span>Send WhatsApp Message Instantly</span>
                </a>
                <button type="button" className={styles.btnSecondary} style={{ width: "100%", justifyContent: "center" }} onClick={closeBookingModal}>
                  <span>Return to Homepage</span>
                </button>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Floating Emergency Action Dock (Quick Contact) */}
      <div className={styles.floatingDock}>
        <a href={`tel:+${WHATSAPP_NUMBER}`} className={styles.dockItem} style={{ color: "var(--mt-accent-cyan)" }}>
          <i className="fa-solid fa-phone" />
          <span>Call Us</span>
        </a>
        <div className={styles.dockDivider} />
        <a
          href={`https://wa.me/${WHATSAPP_NUMBER}?text=${encodeURIComponent("Hello The Mentalist team, I would like to inquire about your counseling services.")}`}
          target="_blank"
          rel="noreferrer"
          className={styles.dockItem}
          style={{ color: "#25d366" }}
        >
          <i className="fa-brands fa-whatsapp" />
          <span>WhatsApp</span>
        </a>
        <div className={styles.dockDivider} />
        <button type="button" className={styles.dockItem} style={{ background: "var(--mt-accent-blue)", color: "#ffffff" }} onClick={() => openBookingModal()}>
          <i className="fa-solid fa-calendar-plus" />
          <span>Book Session</span>
        </button>
      </div>

      {/* Footer */}
      <footer className={styles.footer}>
        <div className={styles.container}>
          <div className={styles.footerGrid}>
            <div className={styles.footerBrand}>
              <a href="#hero" className={styles.navBrand}>
                <div className={styles.brandIcon}>
                  <i className="fa-solid fa-brain" />
                </div>
                <span>THE <span className={styles.textGradient}>MENTALIST</span></span>
              </a>
              <p style={{ color: "var(--mt-text-secondary)", fontSize: "0.95rem", marginTop: 12, maxWidth: 320 }}>
                Guiding minds, shaping futures. Certified coaching, professional psychometric
                testing, and career counseling for a successful life journey.
              </p>
            </div>

            <div>
              <h4 className={styles.footerTitle}>Assessments</h4>
              <ul className={styles.footerLinks}>
                <li><a href="#assessments" className={styles.footerLink}>Student Intelligence</a></li>
                <li><a href="#assessments" className={styles.footerLink}>IQ Plus Evaluation</a></li>
                <li><a href="#assessments" className={styles.footerLink}>Mental Skills Resilience</a></li>
                <li><a href="#assessments" className={styles.footerLink}>Overseas Aptitude</a></li>
                <li><a href="#assessments" className={styles.footerLink}>Brain Benchmark Profiling</a></li>
              </ul>
            </div>

            <div>
              <h4 className={styles.footerTitle}>Jalandhar Center</h4>
              <ul className={styles.footerLinks} style={{ color: "var(--mt-text-secondary)", fontSize: "0.95rem" }}>
                <li>Radisson Enclave Upside,</li>
                <li>Jalandhar Fitness Center,</li>
                <li>Opp. D.A.V. College, Jalandhar.</li>
                <li style={{ marginTop: 8, fontWeight: 700, color: "var(--mt-text-primary)" }}>Mon - Sat: 9:30 AM - 7:00 PM</li>
              </ul>
            </div>

            <div>
              <h4 className={styles.footerTitle}>Connect</h4>
              <ul className={styles.footerLinks}>
                <li><a href={`tel:+${WHATSAPP_NUMBER}`} className={styles.footerLink}><i className="fa-solid fa-phone" /> {PHONE_DISPLAY}</a></li>
                <li><a href="mailto:thementalistofficial21@gmail.com" className={styles.footerLink}><i className="fa-solid fa-envelope" /> Email Us</a></li>
                <li><a href="https://instagram.com/Official_thementalist" target="_blank" rel="noreferrer" className={styles.footerLink}><i className="fa-brands fa-instagram" /> Instagram</a></li>
              </ul>
            </div>
          </div>

          <div className={styles.footerBottom}>
            <div>&copy; {todayYear} The Mentalist. All Rights Reserved.</div>
            <div style={{ display: "flex", gap: 24 }}>
              <span className={styles.footerLink}>Privacy Policy</span>
              <span className={styles.footerLink}>Terms of Service</span>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default LandingPage;
