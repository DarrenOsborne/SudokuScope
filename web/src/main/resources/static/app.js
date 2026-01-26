const grid = document.getElementById('grid');
const solutionGrid = document.getElementById('solutionGrid');
const statusEl = document.getElementById('status');
const statValid = document.getElementById('statValid');
const statStatus = document.getElementById('statStatus');
const statSolutions = document.getElementById('statSolutions');
const statUnique = document.getElementById('statUnique');
const analyzeButton = document.getElementById('analyzeBoard');
const clearButton = document.getElementById('clearBoard');
const loadExampleButton = document.getElementById('loadExample');
const tabButtons = document.querySelectorAll('.tab-button');
const tabPanels = document.querySelectorAll('.tab-panel');

const targetGrid = document.getElementById('targetGrid');
const targetStatus = document.getElementById('targetStatus');
const targetSolutions = document.getElementById('targetSolutions');
const targetApprox = document.getElementById('targetApprox');
const targetDelta = document.getElementById('targetDelta');
const targetElapsed = document.getElementById('targetElapsed');
const targetSearchButton = document.getElementById('targetSearch');
const targetClearButton = document.getElementById('targetClear');
const targetInput = document.getElementById('targetInput');
const targetTime = document.getElementById('targetTime');
const targetSeed = document.getElementById('targetSeed');

function buildGrid(container, readOnly) {
  container.innerHTML = '';
  for (let i = 0; i < 81; i += 1) {
    const input = document.createElement('input');
    input.className = 'cell';
    if (Math.floor(i / 9) % 3 === 0) input.classList.add('block-top');
    if (i % 9 % 3 === 0) input.classList.add('block-left');
    input.inputMode = 'numeric';
    input.maxLength = 1;
    input.dataset.index = i;
    if (readOnly) {
      input.setAttribute('readonly', 'readonly');
    } else {
      input.addEventListener('input', onCellInput);
    }
    container.appendChild(input);
  }
}

function onCellInput(event) {
  const value = event.target.value.replace(/[^1-9]/g, '');
  event.target.value = value;
}

function readBoard() {
  const values = [];
  grid.querySelectorAll('.cell').forEach((cell) => {
    const num = parseInt(cell.value, 10);
    values.push(Number.isNaN(num) ? 0 : num);
  });
  return values;
}

function setBoard(values) {
  const cells = grid.querySelectorAll('.cell');
  values.forEach((value, index) => {
    cells[index].value = value === 0 ? '' : String(value);
  });
}

function setSolution(values) {
  const cells = solutionGrid.querySelectorAll('.cell');
  if (!values) {
    cells.forEach((cell) => {
      cell.value = '';
    });
    return;
  }
  values.forEach((value, index) => {
    cells[index].value = value === 0 ? '' : String(value);
  });
}

function setStatus(message) {
  statusEl.textContent = message;
}

function setTargetStatus(message) {
  targetStatus.textContent = message;
}

function resetSolveStats() {
  statValid.textContent = '-';
  statStatus.textContent = '-';
  statSolutions.textContent = '-';
  statUnique.textContent = '-';
}

function sanitizeDigitsInput(event) {
  const value = event.target.value.replace(/[^0-9]/g, '');
  event.target.value = value;
}

function sanitizeDecimalInput(event) {
  const value = event.target.value.replace(/[^0-9.]/g, '');
  const normalized = value.replace(/(\..*)\./g, '$1');
  event.target.value = normalized;
}

async function analyzeBoard() {
  const cells = readBoard();
  setStatus('Analyzing...');
  try {
    const response = await fetch('api/analyze', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ cells }),
    });

    if (!response.ok) {
      const text = await response.text();
      throw new Error(text || 'Request failed');
    }

    const data = await response.json();
    statValid.textContent = data.valid ? 'Yes' : 'No';
    statStatus.textContent = data.status || '-';
    statSolutions.textContent =
      data.solutionCount ? (data.limitReached ? `~${data.solutionCount}` : data.solutionCount) : '-';
    statUnique.textContent = data.unique ? 'Yes' : 'No';
    setSolution(data.exemplarSolution);
    setStatus(data.message || 'Analysis complete.');
  } catch (error) {
    setStatus(error.message || 'Unable to analyze board.');
  }
}

function setTargetBoard(values) {
  const cells = targetGrid.querySelectorAll('.cell');
  values.forEach((value, index) => {
    cells[index].value = value === 0 ? '' : String(value);
  });
}

function clearTargetResult(message = 'Cleared.') {
  setTargetBoard(Array(81).fill(0));
  targetSolutions.textContent = '-';
  targetApprox.textContent = '-';
  targetDelta.textContent = '-';
  targetElapsed.textContent = '-';
  setTargetStatus(message);
}

function parseTarget() {
  const raw = targetInput.value.trim();
  if (!raw) {
    setTargetStatus('Enter a target solution count.');
    return null;
  }
  if (!/^\d+$/.test(raw)) {
    setTargetStatus('Target must be a positive integer.');
    return null;
  }
  if (raw === '0') {
    setTargetStatus('Target must be greater than zero.');
    return null;
  }
  return raw;
}

function parseTimeLimitMs() {
  const raw = targetTime.value.trim();
  if (!raw) {
    return null;
  }
  const seconds = Number.parseFloat(raw);
  if (Number.isNaN(seconds) || seconds <= 0) {
    return null;
  }
  return Math.round(seconds * 1000);
}

function parseSeed() {
  const raw = targetSeed.value.trim();
  if (!raw) {
    return null;
  }
  if (!/^\d+$/.test(raw)) {
    setTargetStatus('Seed must be a number.');
    return null;
  }
  return Number.parseInt(raw, 10);
}

async function runTargetSearch() {
  const target = parseTarget();
  if (!target) {
    return;
  }
  const timeLimitMs = parseTimeLimitMs();
  const seed = parseSeed();
  setTargetStatus('Searching for closest puzzle...');
  try {
    const response = await fetch('api/target', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        target,
        timeLimitMs,
        seed,
      }),
    });

    if (!response.ok) {
      const text = await response.text();
      throw new Error(text || 'Request failed');
    }

    const data = await response.json();
    setTargetBoard(data.board || Array(81).fill(0));
    const solutionText = data.solutionCount || '-';
    targetSolutions.textContent = data.approximate ? `~${solutionText}` : solutionText;
    targetApprox.textContent = data.approximate ? 'Yes' : 'No';
    targetDelta.textContent = data.delta || '-';
    targetElapsed.textContent = data.elapsedMillis ? `${data.elapsedMillis}ms` : '-';
    setTargetStatus(data.message || 'Search complete.');
  } catch (error) {
    setTargetStatus(error.message || 'Unable to search.');
  }
}

let lastExampleSeed = null;

function pickExampleSeed() {
  let seed = Math.floor(Math.random() * 1_000_000_000);
  if (seed === lastExampleSeed) {
    seed = (seed + Math.floor(Math.random() * 9999) + 1) % 1_000_000_000;
  }
  lastExampleSeed = seed;
  return seed;
}

async function loadRandomExample() {
  const minSolutions = 100;
  const maxSolutions = 400;
  const maxAttempts = 3;
  setStatus('Loading a random example...');
  setSolution(null);
  resetSolveStats();

  for (let attempt = 0; attempt < maxAttempts; attempt += 1) {
    const target = Math.floor(Math.random() * (maxSolutions - minSolutions + 1)) + minSolutions;
    const seed = pickExampleSeed();
    try {
      const response = await fetch('api/target', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ target: String(target), timeLimitMs: 500, seed }),
      });

      if (!response.ok) {
        continue;
      }

      const data = await response.json();
      if (!data.board || data.board.length !== 81) {
        continue;
      }

      setBoard(data.board);
      setStatus('Loaded a random example. Click Analyze to inspect solutions.');
      return;
    } catch (error) {
      // Try again on transient errors.
    }
  }

  setStatus('Unable to fetch a random puzzle. Try again in a moment.');
}

clearButton.addEventListener('click', () => {
  setBoard(Array(81).fill(0));
  setSolution(null);
  setStatus('Cleared.');
});

loadExampleButton.addEventListener('click', loadRandomExample);

analyzeButton.addEventListener('click', analyzeBoard);
targetSearchButton.addEventListener('click', runTargetSearch);
targetClearButton.addEventListener('click', clearTargetResult);
targetInput.addEventListener('input', sanitizeDigitsInput);
targetSeed.addEventListener('input', sanitizeDigitsInput);
targetTime.addEventListener('input', sanitizeDecimalInput);

tabButtons.forEach((button) => {
  button.addEventListener('click', () => {
    const target = button.dataset.tab;
    tabButtons.forEach((btn) => btn.classList.toggle('active', btn === button));
    tabButtons.forEach((btn) =>
      btn.setAttribute('aria-selected', btn === button ? 'true' : 'false'),
    );
    tabPanels.forEach((panel) => {
      panel.classList.toggle('active', panel.id === `tab-${target}`);
    });
  });
});

buildGrid(grid, false);
buildGrid(solutionGrid, true);
buildGrid(targetGrid, true);
setBoard(Array(81).fill(0));
clearTargetResult('Ready.');
