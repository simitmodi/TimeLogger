// TimeLogger Web Edition Controller
(function() {
  // State
  let subjects = [];
  let sessions = [];
  let activeSession = null;
  let dailyGoalMinutes = 120;
  let currentTheme = 'LIGHT';

  // Stopwatch state
  let swRunning = false;
  let swStarted = false;
  let swElapsedSec = 0;
  let swStartTime = null;
  let swInterval = null;
  let swPauses = 0;

  // Timer state
  let timerRunning = false;
  let timerStarted = false;
  let timerTotalSec = 0;
  let timerRemainingSec = 0;
  let timerStartTime = null;
  let timerInterval = null;
  let timerPauses = 0;

  // Pending log action
  let pendingLogAction = null;

  // DOM Elements
  const tabs = document.querySelectorAll('.tab-btn');
  const tabPanes = document.querySelectorAll('.tab-pane');
  const themeSelect = document.getElementById('themeSelect');
  const goalText = document.getElementById('goalText');
  const goalProgressFill = document.getElementById('goalProgressFill');

  // Stopwatch elements
  const swClock = document.getElementById('stopwatchClock');
  const swSubjectDisplay = document.getElementById('stopwatchSubjectDisplay');
  const swTargetCountdown = document.getElementById('swTargetCountdown');
  const swSubject = document.getElementById('swSubject');
  const swActivityType = document.getElementById('swActivityType');
  const swGeneralGroup = document.getElementById('swGeneralGroup');
  const swQuestionsGroup = document.getElementById('swQuestionsGroup');
  const swLectureGroup = document.getElementById('swLectureGroup');
  const swRevisionGroup = document.getElementById('swRevisionGroup');
  const swActivityDetail = document.getElementById('swActivityDetail');
  const swQuestionType = document.getElementById('swQuestionType');
  const swQuestionTopic = document.getElementById('swQuestionTopic');
  const swLectureChapter = document.getElementById('swLectureChapter');
  const swLectureNum = document.getElementById('swLectureNum');
  const swRevisionTopic = document.getElementById('swRevisionTopic');

  const btnSwStart = document.getElementById('btnSwStart');
  const btnSwPause = document.getElementById('btnSwPause');
  const btnSwStop = document.getElementById('btnSwStop');
  const btnSwReset = document.getElementById('btnSwReset');

  // Timer elements
  const timerClock = document.getElementById('timerClock');
  const timerSubjectDisplay = document.getElementById('timerSubjectDisplay');
  const timerSubject = document.getElementById('timerSubject');
  const timerHours = document.getElementById('timerHours');
  const timerMinutes = document.getElementById('timerMinutes');
  const timerSeconds = document.getElementById('timerSeconds');
  const timerInputsGroup = document.getElementById('timerInputsGroup');

  const btnTimerStart = document.getElementById('btnTimerStart');
  const btnTimerPause = document.getElementById('btnTimerPause');
  const btnTimerStop = document.getElementById('btnTimerStop');
  const btnTimerReset = document.getElementById('btnTimerReset');

  // Logs elements
  const logsTableBody = document.getElementById('logsTableBody');
  const logsTableFooter = document.getElementById('logsTableFooter');
  const logsSearch = document.getElementById('logsSearch');
  const logsSubjectFilter = document.getElementById('logsSubjectFilter');
  const logsModeFilter = document.getElementById('logsModeFilter');
  const logsDateFilter = document.getElementById('logsDateFilter');

  // Analysis elements
  const analysisPeriodSelect = document.getElementById('analysisPeriodSelect');
  const btnRefreshAnalysis = document.getElementById('btnRefreshAnalysis');
  const analysisSessionTypeFilter = document.getElementById('analysisSessionTypeFilter');
  const analysisActivitySubjectFilter = document.getElementById('analysisActivitySubjectFilter');
  const analysisRevisionSubjectFilter = document.getElementById('analysisRevisionSubjectFilter');
  const analysisQuestionsSubjectFilter = document.getElementById('analysisQuestionsSubjectFilter');
  const analysisQuestionsTopicFilter = document.getElementById('analysisQuestionsTopicFilter');

  // Drilldown Modal
  const drilldownSessionsModal = document.getElementById('drilldownSessionsModal');
  const drilldownModalTitle = document.getElementById('drilldownModalTitle');
  const drilldownModalCount = document.getElementById('drilldownModalCount');
  const drilldownSessionsBody = document.getElementById('drilldownSessionsBody');
  const btnCloseDrilldownModal = document.getElementById('btnCloseDrilldownModal');
  const btnDismissDrilldownModal = document.getElementById('btnDismissDrilldownModal');

  // Modals
  const recoveryModal = document.getElementById('recoveryModal');
  const recoveryDetails = document.getElementById('recoveryDetails');
  const btnResumeRecovered = document.getElementById('btnResumeRecovered');
  const btnSaveRecovered = document.getElementById('btnSaveRecovered');
  const btnDiscardRecovered = document.getElementById('btnDiscardRecovered');

  const questionsPromptModal = document.getElementById('questionsPromptModal');
  const questionsSolvedCountInput = document.getElementById('questionsSolvedCountInput');
  const btnConfirmQuestions = document.getElementById('btnConfirmQuestions');
  const btnSkipQuestions = document.getElementById('btnSkipQuestions');

  // Init
  window.addEventListener('DOMContentLoaded', () => {
    initTabs();
    initTheme();
    loadAllData();
    initHeartbeat();
    initControls();
    initCalculator();
    initLogsActions();
    initShortcuts();
    initGoalModal();
    initAiCoach();
    initReminders();
  });

  // --- Tab Navigation ---
  function initTabs() {
    tabs.forEach(btn => {
      btn.addEventListener('click', () => {
        tabs.forEach(b => b.classList.remove('active'));
        tabPanes.forEach(p => p.classList.remove('active'));
        btn.classList.add('active');
        const target = document.getElementById('tab-' + btn.dataset.tab);
        if (target) target.classList.add('active');

        if (btn.dataset.tab === 'analysis') renderAnalysis();
        if (btn.dataset.tab === 'insights') renderInsights();
        if (btn.dataset.tab === 'reminders') loadReminders();
        if (btn.dataset.tab === 'ai') loadAiChat();
      });
    });
  }

  // --- Themes ---
  function initTheme() {
    themeSelect.addEventListener('change', () => {
      applyTheme(themeSelect.value);
      fetch('/api/theme', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({theme: themeSelect.value})
      });
    });
  }

  function applyTheme(theme) {
    currentTheme = theme;
    document.body.className = '';
    if (theme === 'DARK') document.body.classList.add('theme-dark');
    else if (theme === 'HIGH_CONTRAST') document.body.classList.add('theme-hc');
    else document.body.classList.add('theme-light');
    themeSelect.value = theme;
  }

  // --- Data Loading ---
  async function loadAllData() {
    try {
      const [subRes, sessRes, goalRes, themeRes, settRes, actRes] = await Promise.all([
        fetch('/api/subjects'),
        fetch('/api/sessions'),
        fetch('/api/goal'),
        fetch('/api/theme'),
        fetch('/api/settings'),
        fetch('/api/active-session')
      ]);

      subjects = await subRes.json();
      sessions = await sessRes.json();
      const goalData = await goalRes.json();
      dailyGoalMinutes = goalData.dailyGoalMinutes || 120;
      const themeData = await themeRes.json();
      applyTheme(themeData.theme || 'LIGHT');
      const settData = await settRes.json();
      document.getElementById('chkNotifications').checked = settData.notificationsEnabled;
      document.getElementById('chkBreakReminders').checked = settData.breakRemindersEnabled;
      document.getElementById('chkRememberBrowser').checked = (settData.preferredLaunchMode === 'BROWSER');

      populateSubjectDropdowns();
      renderSubjectsList();
      renderLogs();
      updateGoalWidget();
      updateTargetCountdown();
      setInterval(updateTargetCountdown, 1000);

      // Check Active Session Recovery
      const actData = await actRes.json();
      if (actData && actData.elapsedSeconds > 0) {
        promptRecovery(actData);
      }
    } catch (e) {
      console.error("Failed to load initial data", e);
    }
  }

  function populateSubjectDropdowns() {
    const fills = [swSubject, timerSubject, logsSubjectFilter, analysisActivitySubjectFilter, analysisRevisionSubjectFilter, analysisQuestionsSubjectFilter];
    fills.forEach(sel => {
      if (!sel) return;
      const current = sel.value;
      sel.innerHTML = '';
      if (sel === logsSubjectFilter || sel === analysisActivitySubjectFilter || sel === analysisRevisionSubjectFilter || sel === analysisQuestionsSubjectFilter) {
        sel.innerHTML = '<option value="All Subjects">All Subjects</option>';
      }
      subjects.forEach(s => {
        const opt = document.createElement('option');
        opt.value = s;
        opt.textContent = s;
        sel.appendChild(opt);
      });
      if (current && (current === 'All Subjects' || subjects.includes(current))) {
        sel.value = current;
      }
    });

    if (swSubject.value) {
      swSubjectDisplay.textContent = 'Subject: ' + swSubject.value;
      loadQuestionTopics(swQuestionType.value, swSubject.value);
      updateStopwatchDetailSuggestions();
    }
    if (timerSubject.value) {
      timerSubjectDisplay.textContent = 'Subject: ' + timerSubject.value;
    }
  }

  function updateTargetCountdown() {
    if (!swTargetCountdown) return;
    const now = new Date();
    const target2026 = new Date(2026, 1, 6, 0, 0, 0); // Feb 6, 2026
    const target2027 = new Date(2027, 1, 6, 0, 0, 0); // Feb 6, 2027
    const target = now > target2026 ? target2027 : target2026;

    const diffMs = Math.max(0, target.getTime() - now.getTime());
    const totalSec = Math.floor(diffMs / 1000);

    const days = Math.floor(totalSec / 86400);
    const hours = Math.floor((totalSec % 86400) / 3600);
    const mins = Math.floor((totalSec % 3600) / 60);
    const secs = totalSec % 60;

    const formattedTarget = target.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
    swTargetCountdown.textContent = `🎯 ${days}d ${hours}h ${mins}m ${secs}s left for ${formattedTarget}`;
  }

  function updateStopwatchDetailSuggestions() {
    const list = document.getElementById('swDetailSuggestions');
    if (!list) return;
    list.innerHTML = '';
    const currentSub = swSubject.value;
    const pastDescs = new Set();
    sessions.forEach(s => {
      if (s.subject === currentSub && s.description) {
        if (!s.description.startsWith('Lecture: ') && !s.description.startsWith('Revision: ') && !s.description.startsWith('Questions: ') && s.description !== 'Timer Session') {
          pastDescs.add(s.description);
        }
      }
    });
    pastDescs.forEach(d => {
      const opt = document.createElement('option');
      opt.value = d;
      list.appendChild(opt);
    });
  }

  async function loadQuestionTopics(type, subject) {
    if (!type || !subject) return;
    try {
      const res = await fetch(`/api/question-topics?type=${encodeURIComponent(type)}&subject=${encodeURIComponent(subject)}`);
      const data = await res.json();
      swQuestionTopic.innerHTML = '';
      data.topics.forEach(t => {
        const opt = document.createElement('option');
        opt.value = t;
        opt.textContent = t;
        swQuestionTopic.appendChild(opt);
      });
      const addOpt = document.createElement('option');
      addOpt.value = '__ADD_NEW__';
      addOpt.textContent = '[Add Custom...]';
      swQuestionTopic.appendChild(addOpt);

      if (data.lastDesc && data.topics.includes(data.lastDesc)) {
        swQuestionTopic.value = data.lastDesc;
      }
    } catch (e) {
      console.error(e);
    }
  }

  async function loadRevisionTopics(subject) {
    if (!subject) return;
    try {
      const res = await fetch(`/api/revision-topics?subject=${encodeURIComponent(subject)}`);
      const data = await res.json();
      swRevisionTopic.innerHTML = '';
      data.topics.forEach(t => {
        const opt = document.createElement('option');
        opt.value = t;
        opt.textContent = t;
        swRevisionTopic.appendChild(opt);
      });
      const addOpt = document.createElement('option');
      addOpt.value = '__ADD_NEW__';
      addOpt.textContent = '[Add Custom...]';
      swRevisionTopic.appendChild(addOpt);

      if (data.lastTopic && data.topics.includes(data.lastTopic)) {
        swRevisionTopic.value = data.lastTopic;
      }
    } catch (e) {
      console.error(e);
    }
  }

  // --- Controls & Activity Type Switching ---
  function initControls() {
    swSubject.addEventListener('change', () => {
      swSubjectDisplay.textContent = 'Subject: ' + swSubject.value;
      loadQuestionTopics(swQuestionType.value, swSubject.value);
      loadRevisionTopics(swSubject.value);
      updateStopwatchDetailSuggestions();
    });

    timerSubject.addEventListener('change', () => {
      timerSubjectDisplay.textContent = 'Subject: ' + timerSubject.value;
    });

    swActivityType.addEventListener('change', () => {
      const val = swActivityType.value;
      swGeneralGroup.classList.add('hidden');
      swQuestionsGroup.classList.add('hidden');
      swLectureGroup.classList.add('hidden');
      swRevisionGroup.classList.add('hidden');

      if (val === 'Questions') {
        swQuestionsGroup.classList.remove('hidden');
        loadQuestionTopics(swQuestionType.value, swSubject.value);
      } else if (val === 'Lecture') {
        swLectureGroup.classList.remove('hidden');
      } else if (val === 'Revision') {
        swRevisionGroup.classList.remove('hidden');
        loadRevisionTopics(swSubject.value);
      } else {
        swGeneralGroup.classList.remove('hidden');
      }
    });

    swQuestionType.addEventListener('change', () => {
      loadQuestionTopics(swQuestionType.value, swSubject.value);
    });

    swQuestionTopic.addEventListener('change', () => {
      if (swQuestionTopic.value === '__ADD_NEW__') {
        const newTopic = prompt("Enter new topic/source name:");
        if (newTopic && newTopic.trim()) {
          const val = newTopic.trim();
          fetch('/api/question-topics', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({type: swQuestionType.value, subject: swSubject.value, topic: val})
          }).then(() => loadQuestionTopics(swQuestionType.value, swSubject.value));
        } else {
          swQuestionTopic.selectedIndex = 0;
        }
      }
    });

    swRevisionTopic.addEventListener('change', () => {
      if (swRevisionTopic.value === '__ADD_NEW__') {
        const newTopic = prompt("Enter new revision topic name:");
        if (newTopic && newTopic.trim()) {
          const val = newTopic.trim();
          fetch('/api/revision-topics', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({subject: swSubject.value, topic: val})
          }).then(() => loadRevisionTopics(swSubject.value));
        } else {
          swRevisionTopic.selectedIndex = 0;
        }
      }
    });

    // Stopwatch Buttons
    btnSwStart.addEventListener('click', startStopwatch);
    btnSwPause.addEventListener('click', togglePauseStopwatch);
    btnSwStop.addEventListener('click', stopStopwatch);
    btnSwReset.addEventListener('click', resetStopwatch);

    // Timer Buttons
    btnTimerStart.addEventListener('click', startTimer);
    btnTimerPause.addEventListener('click', togglePauseTimer);
    btnTimerStop.addEventListener('click', stopTimer);
    btnTimerReset.addEventListener('click', resetTimer);

    // Logs Filters
    logsSearch.addEventListener('input', renderLogs);
    logsSubjectFilter.addEventListener('change', renderLogs);
    logsModeFilter.addEventListener('change', renderLogs);
    logsDateFilter.addEventListener('change', renderLogs);

    // Exports
    document.getElementById('btnExportWeekly').addEventListener('click', () => {
      window.location.href = '/api/export?period=weekly';
    });
    document.getElementById('btnExportMonthly').addEventListener('click', () => {
      window.location.href = '/api/export?period=monthly';
    });

    // Desktop & Exit switches
    document.getElementById('btnSwitchDesktop').addEventListener('click', () => {
      if (confirm("Switch to the Desktop application window?")) {
        fetch('/api/switch-mode', {method: 'POST'});
      }
    });

    document.getElementById('btnExitApp').addEventListener('click', () => {
      const active = swStarted || timerStarted;
      const msg = active
        ? "A study session is currently in progress. Do you really want to close TimeLogger?\n(Your session will be safely preserved for recovery)"
        : "Do you really want to close TimeLogger?";
      if (confirm(msg)) {
        persistActiveSession().then(() => {
          fetch('/api/exit', {method: 'POST'});
          document.body.innerHTML = '<div style="display:flex;height:100vh;align-items:center;justify-content:center;font-family:sans-serif;flex-direction:column;"><h2>TimeLogger closed.</h2><p>You can safely close this browser tab.</p></div>';
        });
      }
    });

    // Settings checkboxes
    document.getElementById('chkNotifications').addEventListener('change', (e) => {
      fetch('/api/settings', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({notificationsEnabled: String(e.target.checked)})
      });
    });
    document.getElementById('chkBreakReminders').addEventListener('change', (e) => {
      fetch('/api/settings', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({breakRemindersEnabled: String(e.target.checked)})
      });
    });
    document.getElementById('chkRememberBrowser').addEventListener('change', (e) => {
      fetch('/api/settings', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({preferredLaunchMode: e.target.checked ? 'BROWSER' : ''})
      });
    });

    // Analysis Filters & Refresh
    if (analysisPeriodSelect) analysisPeriodSelect.addEventListener('change', renderAnalysis);
    if (btnRefreshAnalysis) btnRefreshAnalysis.addEventListener('click', renderAnalysis);
    if (analysisSessionTypeFilter) analysisSessionTypeFilter.addEventListener('change', renderAnalysis);
    if (analysisActivitySubjectFilter) analysisActivitySubjectFilter.addEventListener('change', renderAnalysis);
    if (analysisRevisionSubjectFilter) analysisRevisionSubjectFilter.addEventListener('change', renderAnalysis);
    if (analysisQuestionsSubjectFilter) analysisQuestionsSubjectFilter.addEventListener('change', renderAnalysis);
    if (analysisQuestionsTopicFilter) analysisQuestionsTopicFilter.addEventListener('change', renderAnalysis);

    // Drilldown Sessions Modal
    if (btnCloseDrilldownModal) btnCloseDrilldownModal.addEventListener('click', () => drilldownSessionsModal.classList.add('hidden'));
    if (btnDismissDrilldownModal) btnDismissDrilldownModal.addEventListener('click', () => drilldownSessionsModal.classList.add('hidden'));

    // Add subject
    document.getElementById('btnAddSubject').addEventListener('click', addSubject);
  }

  // --- Stopwatch Logic ---
  function startStopwatch() {
    if (!swStarted) {
      swStarted = true;
      swStartTime = new Date();
      swElapsedSec = 0;
      swPauses = 0;
      setInputsDisabled(true);
    }
    swRunning = true;
    swInterval = setInterval(() => {
      swElapsedSec++;
      swClock.textContent = formatDuration(swElapsedSec);
      updateGoalWidget();
    }, 1000);

    btnSwStart.disabled = true;
    btnSwPause.disabled = false;
    btnSwPause.textContent = 'Pause';
    btnSwStop.disabled = false;
    btnSwReset.disabled = false;
    persistActiveSession();
  }

  function togglePauseStopwatch() {
    if (!swStarted) return;
    if (swRunning) {
      clearInterval(swInterval);
      swRunning = false;
      swPauses++;
      btnSwPause.textContent = 'Resume';
      btnSwStart.disabled = false;
    } else {
      swRunning = true;
      swInterval = setInterval(() => {
        swElapsedSec++;
        swClock.textContent = formatDuration(swElapsedSec);
        updateGoalWidget();
      }, 1000);
      btnSwPause.textContent = 'Pause';
      btnSwStart.disabled = true;
    }
    persistActiveSession();
  }

  function stopStopwatch() {
    if (!swStarted) return;
    if (swRunning) {
      clearInterval(swInterval);
      swRunning = false;
    }

    const actType = swActivityType.value;
    if (actType === 'Questions') {
      const qType = swQuestionType.value;
      if (qType === 'Practice Book Questions' || qType === 'Previous Year Questions') {
        questionsPromptModal.classList.remove('hidden');
        pendingLogAction = (qSolved) => finalizeStopwatchLog(qSolved);
        return;
      }
    }
    finalizeStopwatchLog(0);
  }

  function finalizeStopwatchLog(questionsSolved) {
    const actType = swActivityType.value;
    let actDetail = '';
    if (actType === 'General') actDetail = swActivityDetail.value.trim();
    else if (actType === 'Lecture') actDetail = `Lecture: Ch ${swLectureChapter.value.trim()}, Lec ${swLectureNum.value.trim()}`;
    else if (actType === 'Revision') {
      const rTopic = swRevisionTopic.value === '__ADD_NEW__' ? '' : swRevisionTopic.value;
      actDetail = `Revision: ${rTopic}`;
    }
    else if (actType === 'Questions') {
      const qType = swQuestionType.value;
      const qDesc = swQuestionTopic.value === '__ADD_NEW__' ? '' : swQuestionTopic.value;
      if (questionsSolved > 0) {
        const avg = formatAvgQTime(swElapsedSec / questionsSolved);
        actDetail = `Questions: ${qType}${qDesc ? ', ' + qDesc : ''} (Solved: ${questionsSolved}, Avg Time: ${avg})`;
      } else {
        actDetail = `Questions: ${qType}${qDesc ? ', ' + qDesc : ''}`;
      }
    }

    const payload = {
      type: 'STOPWATCH',
      subject: swSubject.value,
      startTime: (swStartTime || new Date()).toISOString(),
      endTime: new Date().toISOString(),
      durationSeconds: String(Math.max(1, swElapsedSec)),
      description: actDetail,
      pauseCount: String(swPauses),
      questionsSolved: String(questionsSolved)
    };

    fetch('/api/sessions', {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify(payload)
    }).then(async () => {
      await fetch('/api/active-session', {method: 'DELETE'});
      resetStopwatch();
      const res = await fetch('/api/sessions');
      sessions = await res.json();
      renderLogs();
      updateGoalWidget();
      alert("Stopwatch session logged.");
    });
  }

  function resetStopwatch() {
    clearInterval(swInterval);
    swRunning = false;
    swStarted = false;
    swElapsedSec = 0;
    swPauses = 0;
    swStartTime = null;
    swClock.textContent = '00:00:00';
    btnSwStart.disabled = false;
    btnSwPause.disabled = true;
    btnSwPause.textContent = 'Pause';
    btnSwStop.disabled = true;
    btnSwReset.disabled = true;
    setInputsDisabled(false);
    fetch('/api/active-session', {method: 'DELETE'});
  }

  function setInputsDisabled(disabled) {
    swSubject.disabled = disabled;
    swActivityType.disabled = disabled;
    swActivityDetail.disabled = disabled;
    swQuestionType.disabled = disabled;
    swQuestionTopic.disabled = disabled;
    swLectureChapter.disabled = disabled;
    swLectureNum.disabled = disabled;
    swRevisionTopic.disabled = disabled;
  }

  // --- Timer Logic ---
  function startTimer() {
    if (!timerStarted) {
      const h = parseInt(timerHours.value) || 0;
      const m = parseInt(timerMinutes.value) || 0;
      const s = parseInt(timerSeconds.value) || 0;
      timerTotalSec = h * 3600 + m * 60 + s;
      timerRemainingSec = timerTotalSec;

      if (timerTotalSec <= 0) {
        alert("Please set a time greater than 0.");
        return;
      }

      timerStarted = true;
      timerStartTime = new Date();
      timerPauses = 0;
      timerSubject.disabled = true;
      timerInputsGroup.classList.add('hidden');
    }

    timerRunning = true;
    timerInterval = setInterval(() => {
      timerRemainingSec--;
      timerClock.textContent = formatDuration(Math.max(0, timerRemainingSec));
      updateGoalWidget();

      if (timerRemainingSec <= 0) {
        clearInterval(timerInterval);
        timerRunning = false;
        alert("Timer finished! Logging session.");
        finalizeTimerLog(timerTotalSec);
      }
    }, 1000);

    btnTimerStart.disabled = true;
    btnTimerPause.disabled = false;
    btnTimerPause.textContent = 'Pause';
    btnTimerStop.disabled = false;
    btnTimerReset.disabled = false;
    persistActiveSession();
  }

  function togglePauseTimer() {
    if (!timerStarted) return;
    if (timerRunning) {
      clearInterval(timerInterval);
      timerRunning = false;
      timerPauses++;
      btnTimerPause.textContent = 'Resume';
      btnTimerStart.disabled = false;
    } else {
      timerRunning = true;
      timerInterval = setInterval(() => {
        timerRemainingSec--;
        timerClock.textContent = formatDuration(Math.max(0, timerRemainingSec));
        updateGoalWidget();
        if (timerRemainingSec <= 0) {
          clearInterval(timerInterval);
          timerRunning = false;
          alert("Timer finished! Logging session.");
          finalizeTimerLog(timerTotalSec);
        }
      }, 1000);
      btnTimerPause.textContent = 'Pause';
      btnTimerStart.disabled = true;
    }
    persistActiveSession();
  }

  function stopTimer() {
    if (!timerStarted) return;
    if (timerRunning) {
      clearInterval(timerInterval);
      timerRunning = false;
    }
    const elapsed = timerTotalSec - timerRemainingSec;
    if (elapsed > 0) {
      finalizeTimerLog(elapsed);
    } else {
      resetTimer();
    }
  }

  function finalizeTimerLog(elapsedSec) {
    const payload = {
      type: 'TIMER',
      subject: timerSubject.value || 'General',
      startTime: (timerStartTime || new Date()).toISOString(),
      endTime: new Date().toISOString(),
      durationSeconds: String(elapsedSec),
      description: 'Timer Session',
      pauseCount: String(timerPauses),
      questionsSolved: '0'
    };

    fetch('/api/sessions', {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify(payload)
    }).then(async () => {
      await fetch('/api/active-session', {method: 'DELETE'});
      resetTimer();
      const res = await fetch('/api/sessions');
      sessions = await res.json();
      renderLogs();
      updateGoalWidget();
      alert("Timer session logged.");
    });
  }

  function resetTimer() {
    clearInterval(timerInterval);
    timerRunning = false;
    timerStarted = false;
    timerTotalSec = 0;
    timerRemainingSec = 0;
    timerPauses = 0;
    timerClock.textContent = '00:00:00';
    btnTimerStart.disabled = false;
    btnTimerPause.disabled = true;
    btnTimerPause.textContent = 'Pause';
    btnTimerStop.disabled = true;
    btnTimerReset.disabled = true;
    timerSubject.disabled = false;
    timerInputsGroup.classList.remove('hidden');
    fetch('/api/active-session', {method: 'DELETE'});
  }

  // --- Heartbeat & Persistence ---
  function initHeartbeat() {
    setInterval(() => {
      if (swStarted || timerStarted) {
        persistActiveSession();
      }
    }, 5000);
  }

  function persistActiveSession() {
    if (!swStarted && !timerStarted) return Promise.resolve();
    let payload = {};
    if (swStarted) {
      payload = {
        mode: 'STOPWATCH',
        subject: swSubject.value,
        sessionStart: (swStartTime || new Date()).toISOString(),
        elapsedSeconds: String(swElapsedSec),
        pauseCount: String(swPauses),
        isRunning: String(swRunning),
        activityType: swActivityType.value,
        activityDetail: swActivityDetail.value || '',
        timerTotalSeconds: '0',
        timerRemainingSeconds: '0'
      };
    } else {
      payload = {
        mode: 'TIMER',
        subject: timerSubject.value,
        sessionStart: (timerStartTime || new Date()).toISOString(),
        elapsedSeconds: String(timerTotalSec - timerRemainingSec),
        pauseCount: String(timerPauses),
        isRunning: String(timerRunning),
        activityType: 'General',
        activityDetail: '',
        timerTotalSeconds: String(timerTotalSec),
        timerRemainingSeconds: String(timerRemainingSec)
      };
    }

    return fetch('/api/active-session', {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify(payload)
    }).catch(e => console.error(e));
  }

  // --- Session Recovery Prompt ---
  function promptRecovery(state) {
    const isSw = state.mode === 'STOPWATCH';
    const durStr = formatDuration(state.elapsedSeconds);
    recoveryDetails.innerHTML = `
      <table style="width: 100%; border-collapse: collapse; margin-top: 10px; font-size: 14px;">
        <tr><td style="padding: 4px; color: var(--text-muted);">Mode:</td><td style="font-weight: 700;">${isSw ? 'Stopwatch' : 'Timer'}</td></tr>
        <tr><td style="padding: 4px; color: var(--text-muted);">Subject:</td><td style="font-weight: 700;">${state.subject}</td></tr>
        <tr><td style="padding: 4px; color: var(--text-muted);">Duration:</td><td style="font-weight: 700;">${durStr}</td></tr>
        <tr><td style="padding: 4px; color: var(--text-muted);">Activity:</td><td style="font-weight: 700;">${state.activityDetail || state.activityType}</td></tr>
        <tr><td style="padding: 4px; color: var(--text-muted);">Started At:</td><td style="font-weight: 700;">${state.sessionStart}</td></tr>
      </table>
    `;
    recoveryModal.classList.remove('hidden');

    btnResumeRecovered.onclick = () => {
      recoveryModal.classList.add('hidden');
      if (isSw) {
        document.querySelector('[data-tab="stopwatch"]').click();
        swSubject.value = state.subject;
        swSubjectDisplay.textContent = 'Subject: ' + state.subject;
        swStartTime = new Date(state.sessionStart);
        swElapsedSec = state.elapsedSeconds;
        swPauses = state.pauseCount;
        swStarted = true;
        swRunning = false;
        swClock.textContent = durStr;
        btnSwStart.disabled = false;
        btnSwPause.disabled = false;
        btnSwPause.textContent = 'Resume';
        btnSwStop.disabled = false;
        btnSwReset.disabled = false;
        setInputsDisabled(true);
      } else {
        document.querySelector('[data-tab="timer"]').click();
        timerSubject.value = state.subject;
        timerSubjectDisplay.textContent = 'Subject: ' + state.subject;
        timerStartTime = new Date(state.sessionStart);
        timerTotalSec = state.timerTotalSeconds;
        timerRemainingSec = state.timerRemainingSeconds;
        timerPauses = state.pauseCount;
        timerStarted = true;
        timerRunning = false;
        timerClock.textContent = formatDuration(timerRemainingSec);
        btnTimerStart.disabled = false;
        btnTimerPause.disabled = false;
        btnTimerPause.textContent = 'Resume';
        btnTimerStop.disabled = false;
        btnTimerReset.disabled = false;
        timerInputsGroup.classList.add('hidden');
      }
      persistActiveSession();
    };

    btnSaveRecovered.onclick = () => {
      recoveryModal.classList.add('hidden');
      const payload = {
        type: state.mode,
        subject: state.subject,
        startTime: state.sessionStart,
        endTime: new Date().toISOString(),
        durationSeconds: String(state.elapsedSeconds),
        description: state.activityDetail || 'Recovered Session',
        pauseCount: String(state.pauseCount),
        questionsSolved: '0'
      };
      fetch('/api/sessions', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(payload)
      }).then(async () => {
        await fetch('/api/active-session', {method: 'DELETE'});
        const res = await fetch('/api/sessions');
        sessions = await res.json();
        renderLogs();
        updateGoalWidget();
        alert("Interrupted session saved to logs.");
      });
    };

    btnDiscardRecovered.onclick = () => {
      recoveryModal.classList.add('hidden');
      fetch('/api/active-session', {method: 'DELETE'});
    };
  }

  // --- Questions Prompt Actions ---
  btnConfirmQuestions.addEventListener('click', () => {
    const qCount = parseInt(questionsSolvedCountInput.value) || 0;
    questionsPromptModal.classList.add('hidden');
    if (pendingLogAction) pendingLogAction(qCount);
  });
  btnSkipQuestions.addEventListener('click', () => {
    questionsPromptModal.classList.add('hidden');
    if (pendingLogAction) pendingLogAction(0);
  });

  // Selected session in logs
  let selectedSessionIndex = null;

  // --- Logs Table Rendering ---
  function renderLogs() {
    const search = logsSearch.value.toLowerCase().trim();
    const sub = logsSubjectFilter.value;
    const mode = logsModeFilter.value;
    const date = logsDateFilter.value;

    const filtered = sessions.filter(s => {
      if (sub !== 'All' && s.subject !== sub) return false;
      if (mode !== 'All' && s.type !== mode) return false;
      if (search) {
        const text = `${s.subject} ${s.description} ${s.type}`.toLowerCase();
        if (!text.includes(search)) return false;
      }
      if (date !== 'All') {
        const sDate = s.startTime.substring(0, 10);
        const today = new Date().toISOString().substring(0, 10);
        if (date === 'Today' && sDate !== today) return false;
      }
      return true;
    });

    logsTableBody.innerHTML = '';
    filtered.slice().reverse().forEach((s) => {
      const tr = document.createElement('tr');
      if (selectedSessionIndex === s.index) {
        tr.classList.add('selected-row');
      }
      const startFmt = s.startTime.replace('T', ' ').substring(0, 19);
      const endFmt = s.endTime.replace('T', ' ').substring(0, 19);
      const durFmt = formatDuration(s.durationSeconds);
      const dateOnly = s.startTime.substring(0, 10);

      tr.innerHTML = `
        <td>${dateOnly}</td>
        <td><span class="badge-web">${s.type}</span></td>
        <td><b>${s.subject}</b></td>
        <td>${s.description || '-'}</td>
        <td>${startFmt.substring(11)}</td>
        <td>${endFmt.substring(11)}</td>
        <td><b>${durFmt}</b></td>
        <td><button class="btn-delete-item" data-idx="${s.index}">✕</button></td>
      `;

      tr.addEventListener('click', (e) => {
        if (e.target.classList.contains('btn-delete-item')) return;
        selectedSessionIndex = s.index;
        logsTableBody.querySelectorAll('tr').forEach(r => r.classList.remove('selected-row'));
        tr.classList.add('selected-row');
      });

      tr.addEventListener('dblclick', (e) => {
        if (e.target.classList.contains('btn-delete-item')) return;
        selectedSessionIndex = s.index;
        openEditSessionModal(s);
      });

      logsTableBody.appendChild(tr);
    });

    logsTableFooter.textContent = `Showing ${filtered.length} of ${sessions.length} sessions`;

    document.querySelectorAll('.btn-delete-item').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const idx = btn.dataset.idx;
        if (confirm("Delete this session record?")) {
          fetch(`/api/sessions?index=${idx}`, {method: 'DELETE'}).then(async () => {
            const res = await fetch('/api/sessions');
            sessions = await res.json();
            renderLogs();
            updateGoalWidget();
          });
        }
      });
    });
  }

  // --- Goal Widget ---
  function updateGoalWidget() {
    const today = new Date().toISOString().substring(0, 10);
    const todaySessions = sessions.filter(s => s.startTime.startsWith(today));
    let todaySec = todaySessions.reduce((acc, s) => acc + s.durationSeconds, 0);

    if (swStarted && swRunning) todaySec += swElapsedSec;
    if (timerStarted && timerRunning) todaySec += (timerTotalSec - timerRemainingSec);

    const todayMin = Math.floor(todaySec / 60);
    const pct = Math.min(100, Math.round((todayMin / dailyGoalMinutes) * 100));

    goalText.textContent = `Today: ${todayMin} / ${dailyGoalMinutes} min (${pct}%)`;
    goalProgressFill.style.width = pct + '%';
  }

  // --- Analysis Tab Rendering ---
  function getFilteredSessionsForPeriod(period) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    return sessions.filter(s => {
      if (!s.startTime) return false;
      const sDate = new Date(s.startTime);
      sDate.setHours(0, 0, 0, 0);

      switch (period) {
        case "Today":
          return sDate.getTime() === today.getTime();
        case "Yesterday": {
          const yest = new Date(today);
          yest.setDate(today.getDate() - 1);
          return sDate.getTime() === yest.getTime();
        }
        case "This Week": {
          // Monday of current week
          const day = today.getDay();
          const diff = (day === 0 ? -6 : 1) - day;
          const startOfWeek = new Date(today);
          startOfWeek.setDate(today.getDate() + diff);
          return sDate >= startOfWeek && sDate <= today;
        }
        case "Last 7 Days": {
          const past7 = new Date(today);
          past7.setDate(today.getDate() - 6);
          return sDate >= past7 && sDate <= today;
        }
        case "This Month":
          return sDate.getFullYear() === today.getFullYear() && sDate.getMonth() === today.getMonth();
        case "Last 30 Days": {
          const past30 = new Date(today);
          past30.setDate(today.getDate() - 29);
          return sDate >= past30 && sDate <= today;
        }
        case "All Time":
        default:
          return true;
      }
    });
  }

  function calculateGoalStreakStats(targetDate) {
    const dailyGoalMin = dailyGoalMinutes;
    const secondsPerDay = {};

    sessions.forEach(s => {
      const dStr = s.startTime.substring(0, 10);
      secondsPerDay[dStr] = (secondsPerDay[dStr] || 0) + s.durationSeconds;
    });

    const targetDateStr = targetDate.toISOString().substring(0, 10);
    const todayMin = Math.floor((secondsPerDay[targetDateStr] || 0) / 60);
    const todayGoalMet = todayMin >= dailyGoalMin;

    const metGoal = (d) => {
      const dStr = d.toISOString().substring(0, 10);
      return Math.floor((secondsPerDay[dStr] || 0) / 60) >= dailyGoalMin;
    };

    let currentStreak = 0;
    const cur = new Date(targetDate);
    cur.setHours(0, 0, 0, 0);

    if (metGoal(cur)) {
      let d = new Date(cur);
      while (metGoal(d)) {
        currentStreak++;
        d.setDate(d.getDate() - 1);
      }
    } else {
      let d = new Date(cur);
      d.setDate(d.getDate() - 1);
      if (metGoal(d)) {
        while (metGoal(d)) {
          currentStreak++;
          d.setDate(d.getDate() - 1);
        }
      }
    }

    let maxStreak = 0;
    if (sessions.length > 0) {
      let earliestDate = new Date(targetDate);
      sessions.forEach(s => {
        const d = new Date(s.startTime);
        if (d < earliestDate) earliestDate = d;
      });
      earliestDate.setHours(0, 0, 0, 0);

      let running = 0;
      let d = new Date(earliestDate);
      while (d <= cur) {
        if (metGoal(d)) {
          running++;
          if (running > maxStreak) maxStreak = running;
        } else {
          running = 0;
        }
        d.setDate(d.getDate() + 1);
      }
    }

    return {
      todayMinutes: todayMin,
      dailyGoalMinutes: dailyGoalMin,
      todayGoalMet,
      currentStreak,
      maxStreak
    };
  }

  function renderAnalysis() {
    const period = analysisPeriodSelect ? analysisPeriodSelect.value : "Today";
    const filteredSessions = getFilteredSessionsForPeriod(period);

    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const isSingleDay = (period === "Today" || period === "Yesterday");
    const singleDayTargetDate = (period === "Yesterday") ? new Date(today.getTime() - 86400000) : today;

    // 1. Goal & Streak Card
    const goalStats = calculateGoalStreakStats(singleDayTargetDate);
    const goalProgEl = document.getElementById('analysisGoalProgress');
    const goalFillEl = document.getElementById('analysisGoalFill');
    const streakEl = document.getElementById('analysisStreakLabel');

    if (isSingleDay) {
      const prefix = period === "Yesterday" ? "Yesterday" : "Today";
      const pct = goalStats.dailyGoalMinutes > 0 ? Math.round((goalStats.todayMinutes * 100) / goalStats.dailyGoalMinutes) : 0;
      if (goalProgEl) goalProgEl.textContent = `${prefix}: ${goalStats.todayMinutes} / ${goalStats.dailyGoalMinutes} mins (${pct}%)`;
      if (goalFillEl) {
        goalFillEl.style.width = Math.min(100, pct) + '%';
        goalFillEl.style.backgroundColor = goalStats.todayGoalMet ? 'var(--success-color, #2e8b57)' : 'var(--accent-color)';
      }
    } else {
      let daysInPeriod = 1;
      if (period === "This Week") {
        const day = today.getDay();
        daysInPeriod = (day === 0 ? 7 : day);
      } else if (period === "Last 7 Days") {
        daysInPeriod = 7;
      } else if (period === "This Month") {
        daysInPeriod = today.getDate();
      } else if (period === "Last 30 Days") {
        daysInPeriod = 30;
      } else if (period === "All Time") {
        if (sessions.length > 0) {
          let earliest = new Date(today);
          sessions.forEach(s => {
            const d = new Date(s.startTime);
            if (d < earliest) earliest = d;
          });
          daysInPeriod = Math.max(1, Math.round((today - earliest) / (1000 * 60 * 60 * 24)) + 1);
        }
      }
      const totalFilteredSec = filteredSessions.reduce((acc, s) => acc + s.durationSeconds, 0);
      const avgMinutes = totalFilteredSec / (daysInPeriod * 60.0);
      const pct = goalStats.dailyGoalMinutes > 0 ? Math.round((avgMinutes * 100.0) / goalStats.dailyGoalMinutes) : 0;
      if (goalProgEl) goalProgEl.textContent = `Average: ${avgMinutes.toFixed(1)} / ${goalStats.dailyGoalMinutes} mins (${pct}%)`;
      if (goalFillEl) {
        goalFillEl.style.width = Math.min(100, pct) + '%';
        goalFillEl.style.backgroundColor = (avgMinutes >= goalStats.dailyGoalMinutes) ? 'var(--success-color, #2e8b57)' : 'var(--accent-color)';
      }
    }

    if (streakEl) {
      streakEl.textContent = `Streak: ${goalStats.currentStreak} days ${goalStats.currentStreak > 0 ? "🔥" : "💤"} (Max: ${goalStats.maxStreak} 🏆)`;
    }

    // 2. Total Sessions Card (filtered by analysisSessionTypeFilter)
    const typeFilter = analysisSessionTypeFilter ? analysisSessionTypeFilter.value : "All";
    const cardSessions = filteredSessions.filter(s => {
      const desc = s.description || "";
      if (typeFilter === "All") return true;
      if (typeFilter === "Questions") return desc.startsWith("Questions: ");
      if (typeFilter === "Revision") return desc.startsWith("Revision: ");
      if (typeFilter === "Lecture") return desc.startsWith("Lecture: ");
      if (typeFilter === "General") {
        return !desc.startsWith("Questions: ") && !desc.startsWith("Revision: ") && !desc.startsWith("Lecture: ");
      }
      return true;
    });

    const cardSessionsCount = cardSessions.length;
    const cardSeconds = cardSessions.reduce((acc, s) => acc + s.durationSeconds, 0);
    let cardSpanSeconds = 0;
    cardSessions.forEach(s => {
      if (s.startTime && s.endTime) {
        const span = Math.max(0, (new Date(s.endTime) - new Date(s.startTime)) / 1000);
        cardSpanSeconds += span;
      } else {
        cardSpanSeconds += s.durationSeconds;
      }
    });
    const cardEfficiency = cardSpanSeconds > 0 ? ((cardSeconds / cardSpanSeconds) * 100.0) : 100.0;

    document.getElementById('analysisTotalSessions').textContent = cardSessionsCount;
    document.getElementById('analysisAvgSessionDuration').textContent = "Avg Duration: " + (cardSessionsCount > 0 ? formatDuration(Math.round(cardSeconds / cardSessionsCount)) : "00:00:00");
    document.getElementById('analysisStudyEfficiency').textContent = `Study Efficiency: ${cardEfficiency.toFixed(1)}%`;

    // 3. Total Duration Card (Overall filtered)
    const overallSeconds = filteredSessions.reduce((acc, s) => acc + s.durationSeconds, 0);
    document.getElementById('analysisTotalDuration').textContent = formatDuration(overallSeconds);

    const uniqueActiveDays = new Set(filteredSessions.map(s => s.startTime.substring(0, 10))).size;
    const activeDayAvgSec = uniqueActiveDays > 0 ? Math.round(overallSeconds / uniqueActiveDays) : 0;
    document.getElementById('analysisActiveDayAvg').textContent = "Active Day Avg: " + formatDuration(activeDayAvgSec);

    let totalSpanSec = 0;
    filteredSessions.forEach(s => {
      if (s.startTime && s.endTime) {
        totalSpanSec += Math.max(0, (new Date(s.endTime) - new Date(s.startTime)) / 1000);
      } else {
        totalSpanSec += s.durationSeconds;
      }
    });
    const midSessionBreaks = Math.max(0, totalSpanSec - overallSeconds);
    document.getElementById('analysisMidSessionBreaks').textContent = "Mid-Session Breaks: " + formatDuration(midSessionBreaks);

    // Most active subject in current filter
    const subFilteredMap = {};
    filteredSessions.forEach(s => {
      subFilteredMap[s.subject] = (subFilteredMap[s.subject] || 0) + s.durationSeconds;
    });
    let mostActiveName = "None";
    let mostActiveSec = 0;
    Object.entries(subFilteredMap).forEach(([sub, sec]) => {
      if (sec > mostActiveSec) {
        mostActiveSec = sec;
        mostActiveName = sub;
      }
    });
    const mostActiveDisplay = mostActiveSec > 0 ? `${mostActiveName} (${formatDuration(mostActiveSec)})` : "None";
    document.getElementById('analysisMostActiveSubject').textContent = "Most Active: " + (mostActiveDisplay.length > 25 ? mostActiveDisplay.substring(0, 22) + "..." : mostActiveDisplay);

    // 4. Cognitive XP Card & Daily XP Ledger
    const dailyActiveSec = {};
    const dailyBreakSec = {};
    const dailyGainedXp = {};
    const dailyDeductedXp = {};
    const dailyNetXp = {};

    filteredSessions.forEach(s => {
      const dStr = s.startTime.substring(0, 10);
      const active = s.durationSeconds;
      let span = active;
      if (s.startTime && s.endTime) {
        span = Math.max(0, (new Date(s.endTime) - new Date(s.startTime)) / 1000);
      }
      const brk = Math.max(0, span - active);

      dailyActiveSec[dStr] = (dailyActiveSec[dStr] || 0) + active;
      dailyBreakSec[dStr] = (dailyBreakSec[dStr] || 0) + brk;
    });

    let totalXpGained = 0;
    let totalXpDeducted = 0;
    let peakXp = 0;
    let peakDateStr = null;

    Object.keys(dailyActiveSec).forEach(dStr => {
      const active = dailyActiveSec[dStr];
      const brk = dailyBreakSec[dStr];
      const gained = Math.round(active / 60.0);
      const deducted = Math.round((brk / 60.0) * 0.25);
      const net = gained - deducted;

      dailyGainedXp[dStr] = gained;
      dailyDeductedXp[dStr] = deducted;
      dailyNetXp[dStr] = net;

      totalXpGained += gained;
      totalXpDeducted += deducted;

      if (peakDateStr === null || net > peakXp) {
        peakXp = net;
        peakDateStr = dStr;
      }
    });

    const totalNetXp = totalXpGained - totalXpDeducted;
    const activeDaysCount = Object.keys(dailyNetXp).length;
    const avgXpPerDay = activeDaysCount > 0 ? (totalNetXp / activeDaysCount) : 0.0;

    document.getElementById('analysisNetXp').textContent = `${totalNetXp >= 0 ? "+" : ""}${totalNetXp} XP`;
    document.getElementById('analysisXpBreakdown').textContent = `Gained: +${totalXpGained} | Breaks: -${totalXpDeducted}`;
    document.getElementById('analysisXpAnalytics').textContent = peakDateStr ? `Avg: ${avgXpPerDay.toFixed(1)}/day | Peak: ${peakXp} (${peakDateStr})` : `Avg: ${avgXpPerDay.toFixed(1)}/day | Peak: -`;

    // Populate Daily XP Table
    const dailyXpBody = document.getElementById('dailyXpAnalysisBody');
    dailyXpBody.innerHTML = '';
    const sortedDates = Object.keys(dailyNetXp).sort().reverse();
    sortedDates.forEach(dStr => {
      const dObj = new Date(dStr + "T00:00:00");
      const dayName = dObj.toLocaleDateString(undefined, { weekday: 'short' });
      const displayDate = `${dStr} (${dayName})`;
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td><b>${displayDate}</b></td>
        <td>${formatDuration(dailyActiveSec[dStr])}</td>
        <td>${formatDuration(dailyBreakSec[dStr])}</td>
        <td style="color:var(--success-color, #2e8b57);">+${dailyGainedXp[dStr]}</td>
        <td style="color:var(--danger-color, #dc3545);">-${dailyDeductedXp[dStr]}</td>
        <td><b>${dailyNetXp[dStr] >= 0 ? "+" : ""}${dailyNetXp[dStr]}</b></td>
      `;
      tr.addEventListener('dblclick', () => {
        const matches = sessions.filter(s => s.startTime.substring(0, 10) === dStr);
        showSessionsDrilldown(`Sessions for ${displayDate}`, matches);
      });
      dailyXpBody.appendChild(tr);
    });

    // 5. Subject Breakdown Table (All Time)
    const bySubjectAllTime = {};
    sessions.forEach(s => {
      bySubjectAllTime[s.subject] = (bySubjectAllTime[s.subject] || 0) + s.durationSeconds;
    });
    const subBody = document.getElementById('subjectAnalysisBody');
    subBody.innerHTML = '';
    Object.keys(bySubjectAllTime).sort().forEach(sub => {
      const sec = bySubjectAllTime[sub];
      const tr = document.createElement('tr');
      tr.innerHTML = `<td><b>${sub}</b></td><td>${formatDuration(sec)}</td>`;
      tr.addEventListener('dblclick', () => {
        const matches = sessions.filter(s => s.subject.toLowerCase() === sub.toLowerCase());
        showSessionsDrilldown(`Sessions for Subject: ${sub}`, matches);
      });
      subBody.appendChild(tr);
    });

    // 6. Chapter Breakdown Table (All Time)
    const byChapterAllTime = {};
    sessions.forEach(s => {
      const desc = s.description || "";
      if (desc.startsWith("Lecture: ")) {
        let chName = "Unknown Chapter";
        const chStart = desc.indexOf("Ch ");
        if (chStart !== -1) {
          const commaIdx = desc.indexOf(",", chStart);
          chName = (commaIdx !== -1) ? desc.substring(chStart, commaIdx).trim() : desc.substring(chStart).trim();
        }
        const key = `${s.subject} - ${chName}`;
        byChapterAllTime[key] = (byChapterAllTime[key] || 0) + s.durationSeconds;
      }
    });
    const chBody = document.getElementById('chapterAnalysisBody');
    chBody.innerHTML = '';
    Object.keys(byChapterAllTime).sort().forEach(key => {
      const sec = byChapterAllTime[key];
      const tr = document.createElement('tr');
      tr.innerHTML = `<td><b>${key}</b></td><td>${formatDuration(sec)}</td>`;
      tr.addEventListener('dblclick', () => {
        const matches = sessions.filter(s => {
          const desc = s.description || "";
          if (!desc.startsWith("Lecture: ")) return false;
          let chName = "Unknown Chapter";
          const chStart = desc.indexOf("Ch ");
          if (chStart !== -1) {
            const commaIdx = desc.indexOf(",", chStart);
            chName = (commaIdx !== -1) ? desc.substring(chStart, commaIdx).trim() : desc.substring(chStart).trim();
          }
          return `${s.subject} - ${chName}`.toLowerCase() === key.toLowerCase();
        });
        showSessionsDrilldown(`Sessions for Chapter: ${key}`, matches);
      });
      chBody.appendChild(tr);
    });

    // 7. Activity Breakdown Table (Filtered by analysisActivitySubjectFilter)
    const actSubFilter = analysisActivitySubjectFilter ? analysisActivitySubjectFilter.value : "All Subjects";
    const revSubFilter = analysisRevisionSubjectFilter ? analysisRevisionSubjectFilter.value : "All Subjects";
    let dppSec = 0, practiceSec = 0, pyqSec = 0, revSec = 0, genSec = 0;
    let dppQ = 0, practiceQ = 0, pyqQ = 0;
    const byRevTopic = {};

    // 8. Questions Solved by Topic (Filtered by questionsSubjectFilter & questionsTopicFilter)
    const qSubFilter = analysisQuestionsSubjectFilter ? analysisQuestionsSubjectFilter.value : "All Subjects";
    const qTypeFilter = analysisQuestionsTopicFilter ? analysisQuestionsTopicFilter.value : "All (Practice & PYQ)";
    const byQuestionTopicDuration = {};
    const byQuestionTopicSolved = {};

    filteredSessions.forEach(s => {
      const desc = s.description || "";
      const sec = s.durationSeconds;
      const qCount = s.questionsSolved || 0;
      const matchesActSub = (actSubFilter === "All Subjects" || s.subject.toLowerCase() === actSubFilter.toLowerCase());

      if (desc.startsWith("Questions: ")) {
        const qContent = desc.substring("Questions: ".length());
        if (matchesActSub) {
          if (qContent.startsWith("DPP Questions")) {
            dppSec += sec;
            dppQ += qCount;
          } else if (qContent.startsWith("Practice Book Questions")) {
            practiceSec += sec;
            practiceQ += qCount;
          } else if (qContent.startsWith("Previous Year Questions")) {
            pyqSec += sec;
            pyqQ += qCount;
          } else {
            genSec += sec;
          }
        }

        let includeTopic = false;
        if (qTypeFilter === "All (Practice & PYQ)") {
          includeTopic = qContent.startsWith("Practice Book Questions") || qContent.startsWith("Previous Year Questions");
        } else if (qTypeFilter === "Practice Book") {
          includeTopic = qContent.startsWith("Practice Book Questions");
        } else if (qTypeFilter === "Previous Year") {
          includeTopic = qContent.startsWith("Previous Year Questions");
        }

        if (includeTopic) {
          const matchesQSub = (qSubFilter === "All Subjects" || s.subject.toLowerCase() === qSubFilter.toLowerCase());
          if (matchesQSub) {
            let content = qContent;
            const solvedIdx = content.indexOf(" (Solved:");
            if (solvedIdx !== -1) content = content.substring(0, solvedIdx);
            let qDesc = "";
            const commaIdx = content.indexOf(',');
            if (commaIdx !== -1) qDesc = content.substring(commaIdx + 1).trim();
            if (!qDesc) qDesc = "General / Unnamed";

            byQuestionTopicDuration[qDesc] = (byQuestionTopicDuration[qDesc] || 0) + sec;
            byQuestionTopicSolved[qDesc] = (byQuestionTopicSolved[qDesc] || 0) + qCount;
          }
        }
      } else if (desc.startsWith("Revision: ")) {
        if (matchesActSub) {
          revSec += sec;
        }
        const matchesRevSub = (revSubFilter === "All Subjects" || s.subject.toLowerCase() === revSubFilter.toLowerCase());
        if (matchesRevSub) {
          let topic = desc.substring("Revision: ".length()).trim();
          if (!topic) topic = "General/Unnamed";
          byRevTopic[topic] = (byRevTopic[topic] || 0) + sec;
        }
      } else if (!desc.startsWith("Lecture: ")) {
        if (matchesActSub) {
          genSec += sec;
        }
      }
    });

    const actBody = document.getElementById('activityAnalysisBody');
    actBody.innerHTML = '';
    const activitiesList = [
      { name: "DPP Questions", sec: dppSec, q: dppQ },
      { name: "Practice Book Questions", sec: practiceSec, q: practiceQ },
      { name: "Previous Year Questions", sec: pyqSec, q: pyqQ },
      { name: "Revision (Total)", sec: revSec, q: 0 },
      { name: "General / Other", sec: genSec, q: 0 }
    ];
    activitiesList.forEach(item => {
      const avg = item.q > 0 ? formatAvgQTime(item.sec / item.q) : "-";
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td><b>${item.name}</b></td>
        <td>${formatDuration(item.sec)}</td>
        <td>${item.q > 0 ? item.q : "-"}</td>
        <td>${avg}</td>
      `;
      tr.addEventListener('dblclick', () => {
        const matches = filteredSessions.filter(s => {
          if (actSubFilter !== "All Subjects" && s.subject.toLowerCase() !== actSubFilter.toLowerCase()) return false;
          const desc = s.description || "";
          if (item.name === "DPP Questions") return desc.startsWith("Questions: ") && desc.substring(11).startsWith("DPP Questions");
          if (item.name === "Practice Book Questions") return desc.startsWith("Questions: ") && desc.substring(11).startsWith("Practice Book Questions");
          if (item.name === "Previous Year Questions") return desc.startsWith("Questions: ") && desc.substring(11).startsWith("Previous Year Questions");
          if (item.name === "Revision (Total)") return desc.startsWith("Revision: ");
          if (item.name === "General / Other") return !desc.startsWith("Questions: ") && !desc.startsWith("Revision: ") && !desc.startsWith("Lecture: ");
          return false;
        });
        showSessionsDrilldown(`Sessions for Activity: ${item.name}`, matches);
      });
      actBody.appendChild(tr);
    });

    // Populate Revision by Topic Table
    const revBody = document.getElementById('revisionAnalysisBody');
    revBody.innerHTML = '';
    const sortedRev = Object.entries(byRevTopic).sort((a, b) => b[1] - a[1]);
    sortedRev.forEach(([topic, sec]) => {
      const tr = document.createElement('tr');
      tr.innerHTML = `<td><b>${topic}</b></td><td>${formatDuration(sec)}</td>`;
      tr.addEventListener('dblclick', () => {
        const matches = filteredSessions.filter(s => {
          if (revSubFilter !== "All Subjects" && s.subject.toLowerCase() !== revSubFilter.toLowerCase()) return false;
          const desc = s.description || "";
          if (!desc.startsWith("Revision: ")) return false;
          let t = desc.substring("Revision: ".length()).trim();
          if (!t) t = "General/Unnamed";
          return t.toLowerCase() === topic.toLowerCase();
        });
        showSessionsDrilldown(`Sessions for Revision Topic: ${topic}`, matches);
      });
      revBody.appendChild(tr);
    });

    // Populate Questions Solved by Topic Table
    const qTopicBody = document.getElementById('questionsTopicAnalysisBody');
    qTopicBody.innerHTML = '';
    const sortedQTopics = Object.entries(byQuestionTopicDuration).sort((a, b) => b[1] - a[1]);
    sortedQTopics.forEach(([topic, sec]) => {
      const solved = byQuestionTopicSolved[topic] || 0;
      const avg = solved > 0 ? formatAvgQTime(sec / solved) : "-";
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td><b>${topic}</b></td>
        <td>${formatDuration(sec)}</td>
        <td>${solved > 0 ? solved : "-"}</td>
        <td>${avg}</td>
      `;
      tr.addEventListener('dblclick', () => {
        const matches = filteredSessions.filter(s => {
          if (qSubFilter !== "All Subjects" && s.subject.toLowerCase() !== qSubFilter.toLowerCase()) return false;
          const desc = s.description || "";
          if (!desc.startsWith("Questions: ")) return false;
          const qContent = desc.substring("Questions: ".length());

          let includeType = false;
          if (qTypeFilter === "All (Practice & PYQ)") {
            includeType = qContent.startsWith("Practice Book Questions") || qContent.startsWith("Previous Year Questions");
          } else if (qTypeFilter === "Practice Book") {
            includeType = qContent.startsWith("Practice Book Questions");
          } else if (qTypeFilter === "Previous Year") {
            includeType = qContent.startsWith("Previous Year Questions");
          }
          if (!includeType) return false;

          let content = qContent;
          const solvedIdx = content.indexOf(" (Solved:");
          if (solvedIdx !== -1) content = content.substring(0, solvedIdx);
          let qDesc = "";
          const commaIdx = content.indexOf(',');
          if (commaIdx !== -1) qDesc = content.substring(commaIdx + 1).trim();
          if (!qDesc) qDesc = "General / Unnamed";
          return qDesc.toLowerCase() === topic.toLowerCase();
        });
        showSessionsDrilldown(`Sessions for Question Topic: ${topic}`, matches);
      });
      qTopicBody.appendChild(tr);
    });

    drawAnalysisCharts(filteredSessions, period);
  }

  function showSessionsDrilldown(title, matchingSessions) {
    if (!drilldownSessionsModal) return;
    drilldownModalTitle.textContent = title;
    drilldownModalCount.textContent = `Recorded Sessions: ${matchingSessions.length} found`;
    drilldownSessionsBody.innerHTML = '';

    matchingSessions.forEach(s => {
      const tr = document.createElement('tr');
      const startFmt = (s.startTime || "").replace('T', ' ').substring(0, 19);
      tr.innerHTML = `
        <td>${startFmt}</td>
        <td><b>${s.subject}</b></td>
        <td>${formatDuration(s.durationSeconds)}</td>
        <td>${s.description || '-'}</td>
        <td>${s.pauseCount || 0}</td>
        <td>${(s.questionsSolved && s.questionsSolved > 0) ? s.questionsSolved : '-'}</td>
      `;
      drilldownSessionsBody.appendChild(tr);
    });

    drilldownSessionsModal.classList.remove('hidden');
  }

  // --- Insights Tab Rendering ---
  async function renderInsights() {
    try {
      const res = await fetch('/api/insights');
      const d = await res.json();

      document.getElementById('burnoutRiskVal').textContent = d.burnoutRiskPercent + '%';
      const statusEl = document.getElementById('burnoutStatusVal');
      if (d.burnoutRiskPercent >= 70) {
        statusEl.textContent = 'High Burnout Risk! Rest required.';
        statusEl.style.color = 'var(--danger-color)';
      } else if (d.burnoutRiskPercent >= 30) {
        statusEl.textContent = 'Moderate Burnout Risk. Schedule breaks.';
        statusEl.style.color = 'var(--warning-color)';
      } else {
        statusEl.textContent = 'Low Burnout Risk. Doing great!';
        statusEl.style.color = 'var(--accent-color)';
      }

      document.getElementById('prodIndexVal').textContent = d.productivityIndex + '/100';
      document.getElementById('prodProgressFill').style.width = d.productivityIndex + '%';

      // Details
      const grid = document.getElementById('insightsDetailsGrid');
      grid.innerHTML = `
        <div class="detail-row"><span class="detail-label">Total Hours Tracked:</span><span class="detail-val">${d.totalHours} hrs</span></div>
        <div class="detail-row"><span class="detail-label">Consecutive Active Days:</span><span class="detail-val">${d.consecutiveActiveDays} days</span></div>
        <div class="detail-row"><span class="detail-label">Late-Night Sessions:</span><span class="detail-val">${d.lateNightSessions} sessions</span></div>
        <div class="detail-row"><span class="detail-label">Average Break Ratio:</span><span class="detail-val">${d.avgBreakRatioPercent}%</span></div>
        <div class="detail-row"><span class="detail-label">Total Mid-Session Breaks:</span><span class="detail-val">${formatDuration(d.midSessionBreakSeconds)}</span></div>
        <div class="detail-row"><span class="detail-label">Study Efficiency:</span><span class="detail-val">${d.efficiencyPercent}%</span></div>
        <div class="detail-row"><span class="detail-label">Avg Attention Span:</span><span class="detail-val">${d.attentionSpanMin} mins</span></div>
        <div class="detail-row"><span class="detail-label">Focus Score:</span><span class="detail-val">${d.focusScore}/100</span></div>
        <div class="detail-row"><span class="detail-label">7-Day XP:</span><span class="detail-val">${d.totalNetXp} XP</span></div>
        <div class="detail-row"><span class="detail-label">Avg XP / Day:</span><span class="detail-val">${d.avgXpPerDay} XP</span></div>
      `;

      // Warnings
      const wBox = document.getElementById('warningsList');
      wBox.innerHTML = '';
      let hasWarn = false;
      if (parseFloat(d.totalHours) > 56) {
        wBox.innerHTML += `<div class="warning-item">⚠️ Extreme Load: Tracked over 56 hours this week.</div>`;
        hasWarn = true;
      }
      if (parseFloat(d.avgBreakRatioPercent) < 15) {
        wBox.innerHTML += `<div class="warning-item">⚠️ Insufficient Breaks: Break ratio is under 15%. Rest more.</div>`;
        hasWarn = true;
      }
      if (!hasWarn) {
        wBox.innerHTML = `<div class="tip-item">✓ Your tracking habits look balanced and healthy!</div>`;
      }
    } catch (e) {
      console.error(e);
    }
  }

  // --- Subjects Management ---
  function renderSubjectsList() {
    const list = document.getElementById('subjectsList');
    list.innerHTML = '';
    subjects.forEach(s => {
      const li = document.createElement('li');
      li.innerHTML = `<span>${s}</span><button class="btn-delete-item" data-name="${s}">✕</button>`;
      list.appendChild(li);
    });

    list.querySelectorAll('.btn-delete-item').forEach(b => {
      b.addEventListener('click', () => {
        const name = b.dataset.name;
        if (confirm(`Delete subject "${name}"?`)) {
          fetch(`/api/subjects?name=${encodeURIComponent(name)}`, {method: 'DELETE'}).then(async () => {
            const res = await fetch('/api/subjects');
            subjects = await res.json();
            populateSubjectDropdowns();
            renderSubjectsList();
          });
        }
      });
    });
  }

  function addSubject() {
    const input = document.getElementById('newSubjectInput');
    const name = input.value.trim();
    if (!name) return;
    fetch('/api/subjects', {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({name})
    }).then(async () => {
      input.value = '';
      const res = await fetch('/api/subjects');
      subjects = await res.json();
      populateSubjectDropdowns();
      renderSubjectsList();
    });
  }

  // --- Scientific Calculator Modal ---
  function initCalculator() {
    const calcModal = document.getElementById('calcModal');
    const btnCalc = document.getElementById('btnCalculator');
    const btnCloseCalc = document.getElementById('btnCloseCalc');
    const calcDisplay = document.getElementById('calcDisplay');
    const calcHistory = document.getElementById('calcHistory');

    btnCalc.addEventListener('click', () => calcModal.classList.remove('hidden'));
    btnCloseCalc.addEventListener('click', () => calcModal.classList.add('hidden'));

    let expr = '';

    document.querySelectorAll('.c-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const txt = btn.textContent.trim();
        if (txt === 'C') {
          expr = '';
          calcDisplay.value = '0';
          calcHistory.textContent = '';
        } else if (txt === '⌫') {
          expr = expr.slice(0, -1);
          calcDisplay.value = expr || '0';
        } else if (txt === '=') {
          try {
            const sanitized = expr.replace(/×/g, '*').replace(/÷/g, '/').replace(/π/g, 'Math.PI').replace(/e/g, 'Math.E');
            const res = eval(sanitized);
            calcHistory.textContent = expr + ' =';
            calcDisplay.value = res;
            expr = String(res);
          } catch (e) {
            calcDisplay.value = 'Error';
          }
        } else if (txt === 'sin') {
          expr += 'Math.sin(';
          calcDisplay.value = expr;
        } else if (txt === 'cos') {
          expr += 'Math.cos(';
          calcDisplay.value = expr;
        } else if (txt === 'tan') {
          expr += 'Math.tan(';
          calcDisplay.value = expr;
        } else if (txt === '√') {
          expr += 'Math.sqrt(';
          calcDisplay.value = expr;
        } else if (txt === 'ln') {
          expr += 'Math.log(';
          calcDisplay.value = expr;
        } else if (txt === 'log') {
          expr += 'Math.log10(';
          calcDisplay.value = expr;
        } else if (txt === 'x²') {
          expr += '**2';
          calcDisplay.value = expr;
        } else {
          expr += txt;
          calcDisplay.value = expr;
        }
      });
    });
  }

  // --- Analysis Visual Charts (Day of Week & Daily Timeline) ---
  function drawAnalysisCharts(filteredSessions, period) {
    drawDayOfWeekChart(filteredSessions);
    drawDailyTimelineChart(filteredSessions, period);
  }

  function drawDayOfWeekChart(sess) {
    const canvas = document.getElementById('weeklyDayOfWeekChart');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    const w = canvas.width;
    const h = canvas.height;

    ctx.clearRect(0, 0, w, h);

    const dayLabels = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
    const dayTotals = [0, 0, 0, 0, 0, 0, 0]; // Mon=0, Sun=6

    sess.forEach(s => {
      if (!s.startTime) return;
      const d = new Date(s.startTime);
      const jsDay = d.getDay(); // 0 is Sunday, 1 is Monday
      const dayIdx = (jsDay === 0 ? 6 : jsDay - 1);
      dayTotals[dayIdx] += s.durationSeconds;
    });

    let maxSec = Math.max(...dayTotals);
    if (maxSec <= 0) maxSec = 3600;

    const padLeft = 30;
    const padRight = 20;
    const padTop = 25;
    const padBottom = 25;
    const chartW = w - padLeft - padRight;
    const chartH = h - padTop - padBottom;

    // Baseline
    const isDark = currentTheme === 'DARK' || currentTheme === 'HIGH_CONTRAST';
    const textColor = isDark ? '#ffffff' : '#333333';
    const accentColor = isDark ? '#4dabf7' : '#0066cc';
    const gridColor = isDark ? '#444444' : '#e0e0e0';

    ctx.strokeStyle = gridColor;
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(padLeft, h - padBottom);
    ctx.lineTo(w - padRight, h - padBottom);
    ctx.stroke();

    const barGap = 10;
    const numBars = 7;
    const barW = (chartW - (barGap * (numBars - 1))) / numBars;

    for (let i = 0; i < numBars; i++) {
      const sec = dayTotals[i];
      const ratio = sec / maxSec;
      const barH = ratio * chartH;
      const x = padLeft + i * (barW + barGap);
      const y = h - padBottom - barH;

      if (sec > 0) {
        const grad = ctx.createLinearGradient(x, y, x, y + barH);
        grad.addColorStop(0, accentColor);
        grad.addColorStop(1, isDark ? 'rgba(77, 171, 247, 0.25)' : 'rgba(0, 102, 204, 0.25)');
        ctx.fillStyle = grad;
        ctx.beginPath();
        ctx.roundRect(x, y, barW, barH, [4, 4, 0, 0]);
        ctx.fill();

        // Hours text above bar
        const hrs = (sec / 3600).toFixed(1) + 'h';
        ctx.fillStyle = textColor;
        ctx.font = 'bold 10px sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText(hrs, x + barW / 2, y - 5);
      }

      // Day label
      ctx.fillStyle = textColor;
      ctx.font = '11px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText(dayLabels[i], x + barW / 2, h - padBottom + 16);
    }
  }

  function drawDailyTimelineChart(sess, period) {
    const canvas = document.getElementById('dailyTimelineChart');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    const w = canvas.width;
    const h = canvas.height;

    ctx.clearRect(0, 0, w, h);

    const isDark = currentTheme === 'DARK' || currentTheme === 'HIGH_CONTRAST';
    const textColor = isDark ? '#ffffff' : '#333333';
    const gridColor = isDark ? '#444444' : '#e0e0e0';
    const accentColor = isDark ? '#4dabf7' : '#0066cc';

    // 24 hour blocks
    const padLeft = 30;
    const padRight = 20;
    const padTop = 30;
    const padBottom = 25;
    const chartW = w - padLeft - padRight;
    const chartH = h - padTop - padBottom;

    // Grid baseline
    ctx.strokeStyle = gridColor;
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(padLeft, h - padBottom);
    ctx.lineTo(w - padRight, h - padBottom);
    ctx.stroke();

    // Hour markers (every 4 hours: 0, 4, 8, 12, 16, 20, 24)
    ctx.fillStyle = textColor;
    ctx.font = '10px sans-serif';
    ctx.textAlign = 'center';

    for (let hr = 0; hr <= 24; hr += 4) {
      const x = padLeft + (hr / 24.0) * chartW;
      ctx.fillText(`${hr}:00`, x, h - padBottom + 14);
      ctx.beginPath();
      ctx.moveTo(x, h - padBottom);
      ctx.lineTo(x, h - padBottom + 4);
      ctx.stroke();
    }

    // Accumulate study intensity per 15-minute slice across the day (96 slices)
    const slices = new Array(96).fill(0);
    sess.forEach(s => {
      if (!s.startTime || !s.endTime) return;
      const startD = new Date(s.startTime);
      const endD = new Date(s.endTime);
      const startMin = startD.getHours() * 60 + startD.getMinutes();
      let durMin = s.durationSeconds / 60;
      if (durMin <= 0) durMin = 1;

      const sliceStart = Math.floor(startMin / 15);
      const sliceCount = Math.max(1, Math.ceil(durMin / 15));

      for (let i = sliceStart; i < sliceStart + sliceCount && i < 96; i++) {
        slices[i] += durMin;
      }
    });

    const maxSlice = Math.max(...slices);
    if (maxSlice <= 0) {
      ctx.fillStyle = textColor;
      ctx.font = 'italic 12px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText("No sessions in this period", w / 2, h / 2);
      return;
    }

    const sliceW = chartW / 96.0;
    for (let i = 0; i < 96; i++) {
      if (slices[i] > 0) {
        const ratio = Math.min(1.0, slices[i] / maxSlice);
        const barH = Math.max(3, ratio * chartH);
        const x = padLeft + i * sliceW;
        const y = h - padBottom - barH;

        ctx.fillStyle = accentColor;
        ctx.fillRect(x, y, Math.max(1, sliceW - 0.5), barH);
      }
    }
  }

  // --- Logs Actions (Resume, Edit, Clear All, Custom Export) ---
  function initLogsActions() {
    const btnResume = document.getElementById('btnResumeSelectedLog');
    const btnEdit = document.getElementById('btnEditSelectedLog');
    const btnClear = document.getElementById('btnClearAllLogs');
    const btnCustom = document.getElementById('btnExportCustom');

    // Modals
    const editModal = document.getElementById('editSessionModal');
    const btnCloseEditModal = document.getElementById('btnCloseEditModal');
    const btnCancelEditSession = document.getElementById('btnCancelEditSession');
    const btnSaveEditSession = document.getElementById('btnSaveEditSession');

    const customExportModal = document.getElementById('customExportModal');
    const btnCloseCustomExport = document.getElementById('btnCloseCustomExport');
    const btnCancelCustomExport = document.getElementById('btnCancelCustomExport');
    const btnConfirmCustomExport = document.getElementById('btnConfirmCustomExport');

    if (btnResume) {
      btnResume.addEventListener('click', () => {
        if (swStarted || timerStarted) {
          alert("A tracking session is currently active. Please stop or reset your current session before resuming a past log.");
          return;
        }
        if (selectedSessionIndex === null) {
          alert("Please click on a session record in the table first to select it.");
          return;
        }
        const s = sessions.find(item => item.index === selectedSessionIndex);
        if (!s) return;
        resumeSession(s);
      });
    }

    if (btnEdit) {
      btnEdit.addEventListener('click', () => {
        if (selectedSessionIndex === null) {
          alert("Please click on a session record in the table first to select it.");
          return;
        }
        const s = sessions.find(item => item.index === selectedSessionIndex);
        if (!s) return;
        openEditSessionModal(s);
      });
    }

    if (btnClear) {
      btnClear.addEventListener('click', () => {
        if (confirm("Are you sure you want to permanently clear ALL logged study sessions?\nThis action cannot be undone!")) {
          fetch('/api/sessions?all=true', {method: 'DELETE'}).then(async () => {
            const res = await fetch('/api/sessions');
            sessions = await res.json();
            selectedSessionIndex = null;
            renderLogs();
            updateGoalWidget();
            alert("All session logs have been cleared.");
          });
        }
      });
    }

    // Edit Modal controls
    if (btnCloseEditModal) btnCloseEditModal.addEventListener('click', () => editModal.classList.add('hidden'));
    if (btnCancelEditSession) btnCancelEditSession.addEventListener('click', () => editModal.classList.add('hidden'));

    if (btnSaveEditSession) {
      btnSaveEditSession.addEventListener('click', (e) => {
        e.preventDefault();
        const idx = document.getElementById('editSessionIndex').value;
        const payload = {
          subject: document.getElementById('editSessionSubject').value,
          type: document.getElementById('editSessionType').value,
          description: document.getElementById('editSessionDesc').value,
          startTime: document.getElementById('editSessionStart').value,
          endTime: document.getElementById('editSessionEnd').value,
          durationSeconds: document.getElementById('editSessionDuration').value,
          pauseCount: document.getElementById('editSessionPauses').value || '0',
          questionsSolved: document.getElementById('editSessionQuestions').value || '0'
        };

        fetch(`/api/sessions?index=${idx}`, {
          method: 'PUT',
          headers: {'Content-Type': 'application/json'},
          body: JSON.stringify(payload)
        }).then(async res => {
          if (res.ok) {
            editModal.classList.add('hidden');
            const r = await fetch('/api/sessions');
            sessions = await r.json();
            renderLogs();
            updateGoalWidget();
          } else {
            alert("Error saving edited session.");
          }
        });
      });
    }

    // Custom Export controls
    if (btnCustom) {
      btnCustom.addEventListener('click', () => {
        const today = new Date().toISOString().substring(0, 10);
        document.getElementById('exportStartDate').value = today;
        document.getElementById('exportEndDate').value = today;
        customExportModal.classList.remove('hidden');
      });
    }
    if (btnCloseCustomExport) btnCloseCustomExport.addEventListener('click', () => customExportModal.classList.add('hidden'));
    if (btnCancelCustomExport) btnCancelCustomExport.addEventListener('click', () => customExportModal.classList.add('hidden'));
    if (btnConfirmCustomExport) {
      btnConfirmCustomExport.addEventListener('click', () => {
        const start = document.getElementById('exportStartDate').value;
        const end = document.getElementById('exportEndDate').value;
        if (!start || !end) {
          alert("Please choose both start and end dates.");
          return;
        }
        customExportModal.classList.add('hidden');
        window.location.href = `/api/export?period=custom&start=${encodeURIComponent(start)}&end=${encodeURIComponent(end)}`;
      });
    }
  }

  function openEditSessionModal(s) {
    const editModal = document.getElementById('editSessionModal');
    if (!editModal) return;

    // Populate subject select in edit modal
    const editSubSelect = document.getElementById('editSessionSubject');
    editSubSelect.innerHTML = '';
    subjects.forEach(sub => {
      const opt = document.createElement('option');
      opt.value = sub;
      opt.textContent = sub;
      editSubSelect.appendChild(opt);
    });

    document.getElementById('editSessionIndex').value = s.index;
    editSubSelect.value = s.subject;
    document.getElementById('editSessionType').value = s.type;
    document.getElementById('editSessionDesc').value = s.description || '';
    document.getElementById('editSessionStart').value = (s.startTime || '').substring(0, 19);
    document.getElementById('editSessionEnd').value = (s.endTime || '').substring(0, 19);
    document.getElementById('editSessionDuration').value = s.durationSeconds;
    document.getElementById('editSessionPauses').value = s.pauseCount || 0;
    document.getElementById('editSessionQuestions').value = s.questionsSolved || 0;

    editModal.classList.remove('hidden');
  }

  function resumeSession(s) {
    const subject = s.subject;
    const durStr = formatDuration(s.durationSeconds);

    if (s.type === 'STOPWATCH') {
      document.querySelector('[data-tab="stopwatch"]').click();
      if (!subjects.includes(subject)) {
        subjects.push(subject);
        populateSubjectDropdowns();
      }
      swSubject.value = subject;
      swSubjectDisplay.textContent = 'Subject: ' + subject;

      const desc = s.description || '';
      if (desc.startsWith("Questions: ")) {
        swActivityType.value = "Questions";
        swGeneralGroup.classList.add('hidden');
        swQuestionsGroup.classList.remove('hidden');
        swLectureGroup.classList.add('hidden');
        swRevisionGroup.classList.add('hidden');

        const content = desc.substring("Questions: ".length());
        const commaIdx = content.indexOf(',');
        let qType = content;
        let qDesc = '';
        if (commaIdx !== -1) {
          qType = content.substring(0, commaIdx).trim();
          qDesc = content.substring(commaIdx + 1).trim();
          const solvedIdx = qDesc.indexOf(' (Solved:');
          if (solvedIdx !== -1) qDesc = qDesc.substring(0, solvedIdx).trim();
        }
        swQuestionType.value = qType;
        loadQuestionTopics(qType, subject).then(() => {
          if (qDesc) swQuestionTopic.value = qDesc;
        });
      } else if (desc.startsWith("Lecture: ")) {
        swActivityType.value = "Lecture";
        swGeneralGroup.classList.add('hidden');
        swQuestionsGroup.classList.add('hidden');
        swLectureGroup.classList.remove('hidden');
        swRevisionGroup.classList.add('hidden');

        const content = desc.substring("Lecture: ".length());
        const commaIdx = content.indexOf(',');
        let ch = '', lec = '';
        if (commaIdx !== -1) {
          const chPart = content.substring(0, commaIdx).trim();
          const lecPart = content.substring(commaIdx + 1).trim();
          if (chPart.startsWith("Ch ")) ch = chPart.substring(3).trim();
          if (lecPart.startsWith("Lec ")) lec = lecPart.substring(4).trim();
        }
        swLectureChapter.value = ch;
        swLectureNum.value = lec;
      } else if (desc.startsWith("Revision: ")) {
        swActivityType.value = "Revision";
        swGeneralGroup.classList.add('hidden');
        swQuestionsGroup.classList.add('hidden');
        swLectureGroup.classList.add('hidden');
        swRevisionGroup.classList.remove('hidden');
        const rTopic = desc.substring("Revision: ".length()).trim();
        loadRevisionTopics(subject).then(() => {
          if (rTopic) swRevisionTopic.value = rTopic;
        });
      } else {
        swActivityType.value = "General";
        swGeneralGroup.classList.remove('hidden');
        swQuestionsGroup.classList.add('hidden');
        swLectureGroup.classList.add('hidden');
        swRevisionGroup.classList.add('hidden');
        swActivityDetail.value = desc;
      }

      swStartTime = s.startTime ? new Date(s.startTime) : new Date();
      swElapsedSec = s.durationSeconds;
      swPauses = s.pauseCount || 0;
      swStarted = true;
      swRunning = false;
      swClock.textContent = durStr;
      btnSwStart.disabled = false;
      btnSwPause.disabled = false;
      btnSwPause.textContent = 'Resume';
      btnSwStop.disabled = false;
      btnSwReset.disabled = false;
      setInputsDisabled(true);
      persistActiveSession();
    } else {
      document.querySelector('[data-tab="timer"]').click();
      if (!subjects.includes(subject)) {
        subjects.push(subject);
        populateSubjectDropdowns();
      }
      timerSubject.value = subject;
      timerSubjectDisplay.textContent = 'Subject: ' + subject;

      timerStartTime = s.startTime ? new Date(s.startTime) : new Date();
      timerTotalSec = s.durationSeconds;
      timerRemainingSec = s.durationSeconds;
      timerPauses = s.pauseCount || 0;
      timerStarted = true;
      timerRunning = false;
      timerClock.textContent = durStr;
      btnTimerStart.disabled = false;
      btnTimerPause.disabled = false;
      btnTimerPause.textContent = 'Resume';
      btnTimerStop.disabled = false;
      btnTimerReset.disabled = false;
      timerSubject.disabled = true;
      timerInputsGroup.classList.add('hidden');
      persistActiveSession();
    }
  }

  // --- Keyboard Shortcuts Help (F1) & Global Shortcuts ---
  function initShortcuts() {
    const shortcutsModal = document.getElementById('shortcutsModal');
    const btnShortcuts = document.getElementById('btnShortcuts');
    const btnCloseShortcuts = document.getElementById('btnCloseShortcuts');
    const btnDismissShortcuts = document.getElementById('btnDismissShortcuts');

    if (btnShortcuts) btnShortcuts.addEventListener('click', () => shortcutsModal.classList.remove('hidden'));
    if (btnCloseShortcuts) btnCloseShortcuts.addEventListener('click', () => shortcutsModal.classList.add('hidden'));
    if (btnDismissShortcuts) btnDismissShortcuts.addEventListener('click', () => shortcutsModal.classList.add('hidden'));

    window.addEventListener('keydown', (e) => {
      // F1 help
      if (e.key === 'F1') {
        e.preventDefault();
        shortcutsModal.classList.remove('hidden');
        return;
      }

      // Esc closes modals
      if (e.key === 'Escape') {
        document.querySelectorAll('.modal-overlay').forEach(m => m.classList.add('hidden'));
        return;
      }

      // Ignore typing in inputs or textareas
      const tag = (e.target.tagName || '').toLowerCase();
      if (tag === 'input' || tag === 'textarea' || tag === 'select') return;

      // Space: Start/Pause active
      if (e.code === 'Space') {
        e.preventDefault();
        const activeTab = document.querySelector('.tab-btn.active')?.dataset.tab;
        if (activeTab === 'stopwatch') {
          if (!swStarted) startStopwatch();
          else togglePauseStopwatch();
        } else if (activeTab === 'timer') {
          if (!timerStarted) startTimer();
          else togglePauseTimer();
        }
      }

      // Alt + S: Stop
      if (e.altKey && (e.key === 's' || e.key === 'S')) {
        e.preventDefault();
        const activeTab = document.querySelector('.tab-btn.active')?.dataset.tab;
        if (activeTab === 'stopwatch') stopStopwatch();
        else if (activeTab === 'timer') stopTimer();
      }

      // Alt + R: Reset
      if (e.altKey && (e.key === 'r' || e.key === 'R')) {
        e.preventDefault();
        const activeTab = document.querySelector('.tab-btn.active')?.dataset.tab;
        if (activeTab === 'stopwatch') resetStopwatch();
        else if (activeTab === 'timer') resetTimer();
      }

      // Alt + 1..7: Switch Tabs
      if (e.altKey && e.key >= '1' && e.key <= '7') {
        e.preventDefault();
        const tabList = ['stopwatch', 'timer', 'logs', 'analysis', 'insights', 'subjects', 'ai'];
        const targetTab = tabList[parseInt(e.key) - 1];
        if (targetTab) {
          const btn = document.querySelector(`[data-tab="${targetTab}"]`);
          if (btn) btn.click();
        }
      }
    });
  }

  // --- Daily Goal Modal ---
  function initGoalModal() {
    const goalWidget = document.getElementById('goalWidget');
    const btnSetGoal = document.getElementById('btnSetGoal');
    const goalModal = document.getElementById('setGoalModal');
    const btnCloseGoalModal = document.getElementById('btnCloseGoalModal');
    const btnCancelGoal = document.getElementById('btnCancelGoal');
    const btnSaveGoal = document.getElementById('btnSaveGoal');
    const goalHoursInput = document.getElementById('goalHoursInput');
    const goalMinutesInput = document.getElementById('goalMinutesInput');

    function openGoalModal() {
      goalHoursInput.value = Math.floor(dailyGoalMinutes / 60);
      goalMinutesInput.value = dailyGoalMinutes % 60;
      goalModal.classList.remove('hidden');
    }

    if (goalWidget) goalWidget.addEventListener('click', openGoalModal);
    if (btnSetGoal) btnSetGoal.addEventListener('click', openGoalModal);
    if (btnCloseGoalModal) btnCloseGoalModal.addEventListener('click', () => goalModal.classList.add('hidden'));
    if (btnCancelGoal) btnCancelGoal.addEventListener('click', () => goalModal.classList.add('hidden'));

    if (btnSaveGoal) {
      btnSaveGoal.addEventListener('click', () => {
        const hrs = parseInt(goalHoursInput.value) || 0;
        const mins = parseInt(goalMinutesInput.value) || 0;
        const total = hrs * 60 + mins;
        if (total <= 0) {
          alert("Please enter a valid goal greater than 0 minutes.");
          return;
        }
        fetch('/api/goal', {
          method: 'POST',
          headers: {'Content-Type': 'application/json'},
          body: JSON.stringify({dailyGoalMinutes: total})
        }).then(async () => {
          dailyGoalMinutes = total;
          goalModal.classList.add('hidden');
          updateGoalWidget();
          if (document.querySelector('[data-tab="analysis"].active')) {
            renderAnalysis();
          }
        });
      });
    }
  }

  // --- AI Study Coach Tab Controller ---
  function initAiCoach() {
    const modelSelect = document.getElementById('aiModelSelect');
    const btnConfigureKey = document.getElementById('btnAiConfigureKey');
    const btnClearHistory = document.getElementById('btnAiClearHistory');
    const btnSend = document.getElementById('btnAiSendMessage');
    const msgInput = document.getElementById('aiMessageInput');
    const chips = document.querySelectorAll('.ai-chip');

    const keyModal = document.getElementById('aiKeyModal');
    const btnCloseAiKeyModal = document.getElementById('btnCloseAiKeyModal');
    const btnCancelAiKey = document.getElementById('btnCancelAiKey');
    const btnSaveAiKey = document.getElementById('btnSaveAiKey');
    const keyInput = document.getElementById('aiApiKeyInput');

    // Load configured model
    fetch('/api/ai/model').then(r => r.json()).then(d => {
      if (d.model && modelSelect) {
        modelSelect.value = d.model;
      }
    }).catch(e => console.error(e));

    if (modelSelect) {
      modelSelect.addEventListener('change', () => {
        fetch('/api/ai/model', {
          method: 'POST',
          headers: {'Content-Type': 'application/json'},
          body: JSON.stringify({model: modelSelect.value})
        });
      });
    }

    if (btnConfigureKey) {
      btnConfigureKey.addEventListener('click', () => {
        keyInput.value = '';
        keyModal.classList.remove('hidden');
      });
    }

    if (btnCloseAiKeyModal) btnCloseAiKeyModal.addEventListener('click', () => keyModal.classList.add('hidden'));
    if (btnCancelAiKey) btnCancelAiKey.addEventListener('click', () => keyModal.classList.add('hidden'));

    if (btnSaveAiKey) {
      btnSaveAiKey.addEventListener('click', () => {
        const val = keyInput.value.trim();
        fetch('/api/ai/key', {
          method: 'POST',
          headers: {'Content-Type': 'application/json'},
          body: JSON.stringify({key: val})
        }).then(() => {
          keyModal.classList.add('hidden');
          alert("OpenRouter API Key saved successfully.");
        });
      });
    }

    if (btnClearHistory) {
      btnClearHistory.addEventListener('click', () => {
        if (confirm("Clear all AI coaching chat history?")) {
          fetch('/api/ai/clear-chat', {method: 'POST'}).then(() => {
            loadAiChat();
          });
        }
      });
    }

    if (btnSend) {
      btnSend.addEventListener('click', sendAiMessage);
    }

    if (msgInput) {
      msgInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
          e.preventDefault();
          sendAiMessage();
        }
      });
    }

    chips.forEach(chip => {
      chip.addEventListener('click', () => {
        const prompt = chip.dataset.prompt;
        if (msgInput && prompt) {
          msgInput.value = prompt;
          sendAiMessage();
        }
      });
    });
  }

  function loadAiChat() {
    const container = document.getElementById('aiChatMessages');
    if (!container) return;

    fetch('/api/ai/chat').then(r => r.json()).then(history => {
      container.innerHTML = '';
      if (!history || history.length === 0) {
        container.innerHTML = `
          <div style="text-align: center; color: var(--text-muted); margin: auto; padding: 20px;">
            <h3>👋 Welcome to your AI Study Coach!</h3>
            <p style="margin-top: 8px; font-size: 13px;">Ask me anything about your study patterns, revision schedules, or burnout prevention.<br>Select a suggestion chip above or type your question below.</p>
          </div>
        `;
        return;
      }
      history.forEach(turn => {
        appendChatMessage(turn.role, turn.content);
      });
      container.scrollTop = container.scrollHeight;
    }).catch(e => console.error(e));
  }

  function appendChatMessage(role, text) {
    const container = document.getElementById('aiChatMessages');
    if (!container) return;

    const bubble = document.createElement('div');
    bubble.className = `ai-bubble ${role}`;

    if (role === 'assistant') {
      bubble.innerHTML = renderMarkdownSimple(text);
    } else {
      bubble.textContent = text;
    }
    container.appendChild(bubble);
    container.scrollTop = container.scrollHeight;
  }

  function sendAiMessage() {
    const msgInput = document.getElementById('aiMessageInput');
    const btnSend = document.getElementById('btnAiSendMessage');
    const statusBar = document.getElementById('aiStatusBar');
    const msg = msgInput.value.trim();
    if (!msg) return;

    // Check if welcome screen is showing
    const container = document.getElementById('aiChatMessages');
    if (container && container.querySelector('h3')) {
      container.innerHTML = '';
    }

    appendChatMessage('user', msg);
    msgInput.value = '';
    msgInput.disabled = true;
    btnSend.disabled = true;
    statusBar.textContent = 'Thinking... analyzing local study logs...';

    fetch('/api/ai/chat', {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({message: msg})
    }).then(async res => {
      const data = await res.json();
      statusBar.textContent = '';
      msgInput.disabled = false;
      btnSend.disabled = false;
      msgInput.focus();

      if (res.ok && data.reply) {
        appendChatMessage('assistant', data.reply);
      } else {
        const err = data.error || "Unable to get response from AI coach.";
        appendChatMessage('assistant', `⚠️ **Error**: ${err}`);
      }
    }).catch(err => {
      statusBar.textContent = '';
      msgInput.disabled = false;
      btnSend.disabled = false;
      appendChatMessage('assistant', `⚠️ **Connection Error**: ${err.message}`);
    });
  }

  function renderMarkdownSimple(md) {
    if (!md) return '';
    let html = md
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/```([\s\S]*?)```/g, '<pre><code>$1</code></pre>')
      .replace(/`([^`]+)`/g, '<code>$1</code>')
      .replace(/\*\*([^*]+)\*\*/g, '<b>$1</b>')
      .replace(/\*([^*]+)\*/g, '<i>$1</i>')
      .replace(/^### (.*$)/gim, '<div style="font-size:14px;font-weight:700;margin:6px 0 2px;">$1</div>')
      .replace(/^## (.*$)/gim, '<div style="font-size:15px;font-weight:700;margin:8px 0 3px;">$1</div>')
      .replace(/^# (.*$)/gim, '<div style="font-size:16px;font-weight:700;margin:10px 0 4px;">$1</div>')
      .replace(/^\s*[-*]\s+(.*$)/gim, '<li style="margin-left:16px;">$1</li>')
      .replace(/\n\n/g, '<br><br>')
      .replace(/\n/g, '<br>');
    return html;
  }

  // --- Utilities ---
  function formatDuration(sec) {
    if (sec < 0) sec = 0;
    const h = Math.floor(sec / 3600);
    const m = Math.floor((sec % 3600) / 60);
    const s = sec % 60;
    const pad = (n) => String(n).padStart(2, '0');
    return `${pad(h)}:${pad(m)}:${pad(s)}`;
  }

  function formatAvgQTime(sec) {
    const m = Math.floor(sec / 60);
    const s = Math.round(sec % 60);
    if (m === 0) return `${s}s`;
    return `${m}m ${s}s`;
  }

  // --- Apple-Style Reminders Dashboard Implementation ---
  let remindersData = [];
  let currentReminderFilter = 'TODAY';
  let editingReminderId = null;

  function initReminders() {
    const remCards = document.querySelectorAll('.rem-card');
    remCards.forEach(card => {
      card.addEventListener('click', () => {
        remCards.forEach(c => c.classList.remove('active'));
        card.classList.add('active');
        currentReminderFilter = card.dataset.filter;
        renderRemindersList();
      });
    });

    const btnOpenModal = document.getElementById('btnOpenNewReminderModal');
    const modal = document.getElementById('reminderModal');
    const btnCloseModal = document.getElementById('btnCloseReminderModal');
    const btnCancelModal = document.getElementById('btnCancelReminderModal');
    const btnSaveModal = document.getElementById('btnSaveReminderModal');

    if (btnOpenModal) {
      btnOpenModal.addEventListener('click', () => {
        openReminderModal(null);
      });
    }

    if (btnCloseModal) btnCloseModal.addEventListener('click', () => modal.classList.add('hidden'));
    if (btnCancelModal) btnCancelModal.addEventListener('click', () => modal.classList.add('hidden'));

    if (btnSaveModal) {
      btnSaveModal.addEventListener('click', saveReminderFromModal);
    }
  }

  async function loadReminders() {
    try {
      const res = await fetch('/api/reminders');
      if (res.ok) {
        remindersData = await res.json();
        updateReminderBadges();
        renderRemindersList();
      }
    } catch (e) {
      console.error('Failed to load reminders', e);
    }
  }

  function updateReminderBadges() {
    const todayStr = new Date().toISOString().split('T')[0];
    const today = remindersData.filter(r => !r.completed && (r.dueDate === todayStr || (r.dueDate && r.dueDate < todayStr))).length;
    const sched = remindersData.filter(r => !r.completed && r.dueDate).length;
    const all = remindersData.filter(r => !r.completed).length;
    const flagged = remindersData.filter(r => !r.completed && r.flagged).length;
    const comp = remindersData.filter(r => r.completed).length;

    const elToday = document.getElementById('remCountToday');
    const elSched = document.getElementById('remCountScheduled');
    const elAll = document.getElementById('remCountAll');
    const elFlag = document.getElementById('remCountFlagged');
    const elComp = document.getElementById('remCountCompleted');

    if (elToday) elToday.textContent = today;
    if (elSched) elSched.textContent = sched;
    if (elAll) elAll.textContent = all;
    if (elFlag) elFlag.textContent = flagged;
    if (elComp) elComp.textContent = comp;
  }

  function renderRemindersList() {
    const wrapper = document.getElementById('remindersListWrapper');
    const heading = document.getElementById('remindersViewHeading');
    const countText = document.getElementById('remindersViewCount');
    if (!wrapper) return;

    const todayStr = new Date().toISOString().split('T')[0];
    let filtered = [];

    switch (currentReminderFilter) {
      case 'TODAY':
        heading.textContent = 'Today';
        filtered = remindersData.filter(r => !r.completed && (r.dueDate === todayStr || (r.dueDate && r.dueDate < todayStr)));
        break;
      case 'SCHEDULED':
        heading.textContent = 'Scheduled';
        filtered = remindersData.filter(r => !r.completed && r.dueDate);
        break;
      case 'FLAGGED':
        heading.textContent = 'Flagged';
        filtered = remindersData.filter(r => !r.completed && r.flagged);
        break;
      case 'COMPLETED':
        heading.textContent = 'Completed';
        filtered = remindersData.filter(r => r.completed);
        break;
      case 'ALL':
      default:
        heading.textContent = 'All Reminders';
        filtered = remindersData.filter(r => !r.completed);
        break;
    }

    countText.textContent = `${filtered.length} ${filtered.length === 1 ? 'Reminder' : 'Reminders'}`;
    wrapper.innerHTML = '';

    if (filtered.length === 0) {
      wrapper.innerHTML = `
        <div class="rem-empty-box">
          <div class="rem-empty-icon">✨</div>
          <div class="rem-empty-title">No Reminders in ${heading.textContent}</div>
          <p class="text-muted">Click "+ New Reminder" above to add study tasks.</p>
        </div>
      `;
      return;
    }

    // When in TODAY view: group by Day Periods
    if (currentReminderFilter === 'TODAY') {
      const periods = [
        { key: 'MORNING', label: '☀️ Morning (09:00 AM)' },
        { key: 'AFTERNOON', label: '🌤️ Afternoon (02:00 PM)' },
        { key: 'EVENING', label: '🌙 Evening (07:00 PM)' },
        { key: 'NIGHT', label: '🌌 Night (09:30 PM)' },
        { key: 'ALL_DAY', label: '📋 Any Time / All Day' }
      ];

      periods.forEach(p => {
        const groupItems = filtered.filter(item => (item.period || 'ALL_DAY') === p.key);
        if (groupItems.length > 0) {
          const groupEl = document.createElement('div');
          groupEl.className = 'rem-period-group';
          groupEl.innerHTML = `
            <div class="rem-period-heading">${p.label} <span class="text-muted">(${groupItems.length})</span></div>
            <div class="rem-period-box"></div>
          `;
          const box = groupEl.querySelector('.rem-period-box');
          groupItems.forEach(item => box.appendChild(createReminderRowElement(item)));
          wrapper.appendChild(groupEl);
        }
      });
    } else {
      // Apple Scheduled timeline view (Today, Tomorrow, Day by Day, Rest of Month, Future Months)
      renderWebScheduledTimeline(filtered, wrapper);
    }
  }

  function renderWebScheduledTimeline(items, wrapper) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const todayStr = today.toISOString().split('T')[0];

    const tomorrow = new Date(today);
    tomorrow.setDate(today.getDate() + 1);
    const tomorrowStr = tomorrow.toISOString().split('T')[0];

    const nextWeek = new Date(today);
    nextWeek.setDate(today.getDate() + 7);

    // Sort items chronologically
    const sorted = [...items].sort((a, b) => {
      const da = a.dueDate || '';
      const db = b.dueDate || '';
      if (da !== db) return da.localeCompare(db);
      const ta = a.dueTime || '';
      const tb = b.dueTime || '';
      return ta.localeCompare(tb);
    });

    const overdueItems = [];
    const todayItems = [];
    const tomorrowItems = [];
    const dayByDayItems = {};
    const restOfMonthItems = [];
    const futureMonthsItems = {};

    sorted.forEach(r => {
      const dStr = r.dueDate;
      if (!dStr) {
        todayItems.push(r);
        return;
      }
      const d = new Date(dStr + 'T00:00:00');
      if (d < today) {
        overdueItems.push(r);
      } else if (dStr === todayStr) {
        todayItems.push(r);
      } else if (dStr === tomorrowStr) {
        tomorrowItems.push(r);
      } else if (d < nextWeek) {
        if (!dayByDayItems[dStr]) dayByDayItems[dStr] = [];
        dayByDayItems[dStr].push(r);
      } else if (d.getFullYear() === today.getFullYear() && d.getMonth() === today.getMonth()) {
        restOfMonthItems.push(r);
      } else {
        const ymKey = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
        if (!futureMonthsItems[ymKey]) futureMonthsItems[ymKey] = { date: d, list: [] };
        futureMonthsItems[ymKey].list.push(r);
      }
    });

    // Helper to render date section box matching Apple Reminders
    const renderSection = (title, list, isOverdue = false) => {
      if (isOverdue && (!list || list.length === 0)) return;
      const count = list ? list.length : 0;
      const countBadge = count > 0 ? ` <span class="text-muted">(${count})</span>` : '';
      const groupEl = document.createElement('div');
      groupEl.className = 'rem-period-group';
      groupEl.innerHTML = `
        <div class="rem-period-heading" style="${isOverdue ? 'color:#ff453a;' : ''}">
          ${title}${countBadge}
        </div>
        <div class="rem-period-box"></div>
      `;
      const box = groupEl.querySelector('.rem-period-box');
      if (count === 0) {
        box.innerHTML = `
          <div style="padding:10px 16px;display:flex;align-items:center;">
            <span style="width:20px;height:20px;border-radius:50%;border:2px dashed var(--border-color);display:inline-block;opacity:0.6;"></span>
          </div>`;
      } else {
        list.forEach(item => box.appendChild(createReminderRowElement(item)));
      }
      wrapper.appendChild(groupEl);
    };

    if (overdueItems.length > 0) {
      renderSection('⚠️ Overdue', overdueItems, true);
    }
    renderSection('Today', todayItems);
    renderSection('Tomorrow', tomorrowItems);

    // Upcoming 5 individual days
    for (let i = 2; i <= 6; i++) {
      const d = new Date(today);
      d.setDate(today.getDate() + i);
      const dStr = d.toISOString().split('T')[0];
      const dayLabel = d.toLocaleDateString('en-US', { weekday: 'short', day: 'numeric', month: 'short' });
      renderSection(dayLabel, dayByDayItems[dStr] || []);
    }

    // Rest of Month
    const curMonthName = today.toLocaleDateString('en-US', { month: 'long' });
    renderSection(`Rest of ${curMonthName}`, restOfMonthItems);

    // Future 8 Months
    for (let m = 1; m <= 8; m++) {
      const d = new Date(today);
      d.setMonth(today.getMonth() + m);
      const ymKey = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
      const isCurYear = d.getFullYear() === today.getFullYear();
      const mLabel = isCurYear
        ? d.toLocaleDateString('en-US', { month: 'long' })
        : d.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
      const mList = futureMonthsItems[ymKey] ? futureMonthsItems[ymKey].list : [];
      renderSection(mLabel, mList);
    }
  }

  function createReminderRowElement(item) {
    const row = document.createElement('div');
    row.className = `rem-item-row ${item.completed ? 'is-completed' : ''}`;

    let prioBadge = '';
    if (item.priority === 'HIGH') prioBadge = '<span class="rem-priority-badge">!!!</span>';
    else if (item.priority === 'MEDIUM') prioBadge = '<span class="rem-priority-badge">!!</span>';
    else if (item.priority === 'LOW') prioBadge = '<span class="rem-priority-badge">!</span>';

    const flagBadge = item.flagged ? '<span>🚩</span>' : '';
    const subjectPill = item.subject ? `<span class="rem-subject-pill">${escapeHtml(item.subject)}</span>` : '';

    const todayStr = new Date().toISOString().split('T')[0];
    let dueStr = item.dueDate || '';
    if (dueStr === todayStr) dueStr = 'Today';

    let timeStr = item.dueTime ? `at ${item.dueTime}` : '';
    const timeDisplay = dueStr ? `<span>📅 ${dueStr} ${timeStr}</span>` : '';

    row.innerHTML = `
      <button class="rem-circle-check ${item.completed ? 'completed' : ''}" title="Toggle complete">
        ${item.completed ? '✓' : ''}
      </button>
      <div class="rem-item-content">
        <div class="rem-item-title-row">
          <span class="rem-item-title">${escapeHtml(item.title)}</span>
          ${prioBadge}
          ${flagBadge}
        </div>
        <div class="rem-item-meta">
          ${subjectPill}
          ${timeDisplay}
        </div>
        ${item.notes ? `<div class="rem-item-notes">📝 ${escapeHtml(item.notes)}</div>` : ''}
      </div>
      <div class="rem-actions-bar">
        <button class="rem-icon-btn btn-flag" title="Flag">${item.flagged ? '🚩' : '🏳️'}</button>
        <button class="rem-icon-btn btn-edit" title="Edit">✏️</button>
        <button class="rem-icon-btn btn-del" title="Delete">🗑️</button>
      </div>
    `;

    // Event Listeners
    row.querySelector('.rem-circle-check').addEventListener('click', () => toggleReminderCompleted(item));
    row.querySelector('.btn-flag').addEventListener('click', () => toggleReminderFlag(item));
    row.querySelector('.btn-edit').addEventListener('click', () => openReminderModal(item));
    row.querySelector('.btn-del').addEventListener('click', () => deleteReminder(item));

    return row;
  }

  async function toggleReminderCompleted(item) {
    try {
      const res = await fetch('/api/reminders', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ id: item.id, completed: String(!item.completed) })
      });
      if (res.ok) {
        item.completed = !item.completed;
        updateReminderBadges();
        renderRemindersList();
      }
    } catch (e) {
      console.error(e);
    }
  }

  async function toggleReminderFlag(item) {
    try {
      const res = await fetch('/api/reminders', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ id: item.id, flagged: String(!item.flagged) })
      });
      if (res.ok) {
        item.flagged = !item.flagged;
        updateReminderBadges();
        renderRemindersList();
      }
    } catch (e) {
      console.error(e);
    }
  }

  async function deleteReminder(item) {
    if (!confirm(`Delete reminder "${item.title}"?`)) return;
    try {
      const res = await fetch(`/api/reminders?id=${encodeURIComponent(item.id)}`, {
        method: 'DELETE'
      });
      if (res.ok) {
        remindersData = remindersData.filter(r => r.id !== item.id);
        updateReminderBadges();
        renderRemindersList();
      }
    } catch (e) {
      console.error(e);
    }
  }

  function openReminderModal(existing) {
    editingReminderId = existing ? existing.id : null;
    const modal = document.getElementById('reminderModal');
    const modalTitle = document.getElementById('reminderModalTitle');
    const titleInp = document.getElementById('remModalTitleInput');
    const notesInp = document.getElementById('remModalNotesInput');
    const subjectSel = document.getElementById('remModalSubjectSelect');
    const dateInp = document.getElementById('remModalDateInput');
    const timeInp = document.getElementById('remModalTimeInput');
    const prioSel = document.getElementById('remModalPrioritySelect');
    const flagInp = document.getElementById('remModalFlaggedInput');

    // Populate Subjects
    subjectSel.innerHTML = '';
    const defOption = document.createElement('option');
    defOption.value = 'General';
    defOption.textContent = 'General';
    subjectSel.appendChild(defOption);
    subjectsData.forEach(sub => {
      if (sub !== 'General') {
        const opt = document.createElement('option');
        opt.value = sub;
        opt.textContent = sub;
        subjectSel.appendChild(opt);
      }
    });

    const todayStr = new Date().toISOString().split('T')[0];

    if (existing) {
      modalTitle.textContent = 'Edit Reminder';
      titleInp.value = existing.title || '';
      notesInp.value = existing.notes || '';
      subjectSel.value = existing.subject || 'General';
      dateInp.value = existing.dueDate || todayStr;
      timeInp.value = existing.dueTime || '09:00';
      prioSel.value = existing.priority || 'NONE';
      flagInp.checked = Boolean(existing.flagged);
    } else {
      modalTitle.textContent = 'New Reminder';
      titleInp.value = '';
      notesInp.value = '';
      subjectSel.value = 'General';
      dateInp.value = todayStr;
      timeInp.value = '09:00';
      prioSel.value = 'NONE';
      flagInp.checked = false;
    }

    modal.classList.remove('hidden');
    titleInp.focus();
  }

  function derivePeriodFromTime(timeStr) {
    if (!timeStr) return 'ALL_DAY';
    const clean = timeStr.trim().toLowerCase();
    let hour = 9;
    const match = clean.match(/^(\d{1,2}):(\d{2})\s*(am|pm)?$/);
    if (match) {
      hour = parseInt(match[1], 10);
      const isPm = match[3] === 'pm';
      const isAm = match[3] === 'am';
      if (isPm && hour < 12) hour += 12;
      if (isAm && hour === 12) hour = 0;
    } else {
      const parts = clean.split(':');
      if (parts.length > 0) hour = parseInt(parts[0], 10) || 9;
    }
    if (hour >= 5 && hour < 12) return 'MORNING';
    if (hour >= 12 && hour < 17) return 'AFTERNOON';
    if (hour >= 17 && hour < 21) return 'EVENING';
    return 'NIGHT';
  }

  async function saveReminderFromModal() {
    const titleInp = document.getElementById('remModalTitleInput');
    const notesInp = document.getElementById('remModalNotesInput');
    const subjectSel = document.getElementById('remModalSubjectSelect');
    const dateInp = document.getElementById('remModalDateInput');
    const timeInp = document.getElementById('remModalTimeInput');
    const prioSel = document.getElementById('remModalPrioritySelect');
    const flagInp = document.getElementById('remModalFlaggedInput');

    const title = titleInp.value.trim();
    if (!title) {
      alert('Please enter a reminder title');
      return;
    }

    const timeVal = timeInp.value || '09:00';
    const payload = {
      title,
      notes: notesInp.value.trim(),
      subject: subjectSel.value,
      dueDate: dateInp.value,
      dueTime: timeVal,
      period: derivePeriodFromTime(timeVal),
      priority: prioSel.value,
      flagged: String(flagInp.checked)
    };

    try {
      if (editingReminderId) {
        payload.id = editingReminderId;
        const res = await fetch('/api/reminders', {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });
        if (res.ok) {
          await loadReminders();
          document.getElementById('reminderModal').classList.add('hidden');
        }
      } else {
        const res = await fetch('/api/reminders', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });
        if (res.ok) {
          await loadReminders();
          document.getElementById('reminderModal').classList.add('hidden');
        }
      }
    } catch (e) {
      console.error(e);
      alert('Error saving reminder');
    }
  }

})();

