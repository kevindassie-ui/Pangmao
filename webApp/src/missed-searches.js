const STORAGE_KEY = "pangmao.web.missed-searches.v1";
export const MAX_MISSED_SEARCHES = 100;
export const MAX_MISSED_QUERY_LENGTH = 120;

function cleanQuery(value) {
  if (typeof value !== "string") return "";
  const query = value.normalize("NFC").trim().replace(/\s+/gu, " ");
  return query && query.length <= MAX_MISSED_QUERY_LENGTH && /\p{L}/u.test(query)
    ? query : "";
}

function queryKey(query) {
  return query.toLocaleLowerCase("fr").replaceAll("’", "'");
}

function validDate(value) {
  return typeof value === "string" && Number.isFinite(Date.parse(value));
}

export function sanitizeMissedSearchJournal(value) {
  const entries = [];
  const seen = new Set();
  for (const entry of Array.isArray(value?.entries) ? value.entries : []) {
    const query = cleanQuery(entry?.query);
    const key = queryKey(query);
    if (!query || seen.has(key) || !validDate(entry.firstSeen) ||
        !validDate(entry.lastSeen) || Date.parse(entry.firstSeen) > Date.parse(entry.lastSeen) ||
        !Number.isSafeInteger(entry.count) || entry.count < 1) continue;
    seen.add(key);
    entries.push({
      query,
      language: /\p{Script=Han}/u.test(query) ? "zh" : "fr",
      count: entry.count,
      firstSeen: new Date(entry.firstSeen).toISOString(),
      lastSeen: new Date(entry.lastSeen).toISOString(),
    });
    if (entries.length === MAX_MISSED_SEARCHES) break;
  }
  return { enabled: value?.enabled === true, entries };
}

export function loadMissedSearchJournal(storage) {
  try {
    const target = storage ?? window.localStorage;
    return sanitizeMissedSearchJournal(JSON.parse(target.getItem(STORAGE_KEY) ?? "null"));
  } catch {
    return { enabled: false, entries: [] };
  }
}

export function saveMissedSearchJournal(journal, storage) {
  try {
    const target = storage ?? window.localStorage;
    target.setItem(STORAGE_KEY, JSON.stringify(sanitizeMissedSearchJournal(journal)));
    return true;
  } catch {
    return false;
  }
}

export function recordMissedSearch(journal, value, now = new Date()) {
  const query = cleanQuery(value);
  if (!journal.enabled || !query) return journal;
  const key = queryKey(query);
  const previous = journal.entries.find((entry) => queryKey(entry.query) === key);
  const timestamp = new Date(Math.max(now.getTime(), Date.parse(previous?.lastSeen) || 0)).toISOString();
  const entry = {
    query,
    language: /\p{Script=Han}/u.test(query) ? "zh" : "fr",
    count: previous ? Math.min(previous.count + 1, Number.MAX_SAFE_INTEGER) : 1,
    firstSeen: previous?.firstSeen ?? timestamp,
    lastSeen: timestamp,
  };
  return {
    enabled: true,
    entries: [entry, ...journal.entries.filter((item) => queryKey(item.query) !== key)]
      .slice(0, MAX_MISSED_SEARCHES),
  };
}

// A completed attempt is recorded at most once. Any newer search, clear or
// preference change invalidates it, including a delayed Chinese shard lookup.
export function createMissedSearchRecorder({ getJournal, onRecord }) {
  let activeAttempt = null;
  return {
    invalidate() { activeAttempt = null; },
    begin(query, submitted = false) {
      activeAttempt = { query, submitted, enabled: getJournal().enabled };
      return activeAttempt;
    },
    finish(attempt, { resultCount, fallbackResultCount = 0, fallbackFailed = false }) {
      if (attempt !== activeAttempt) return false;
      activeAttempt = null;
      if (!attempt.submitted || !attempt.enabled || fallbackFailed ||
          resultCount !== 0 || fallbackResultCount !== 0) return false;
      const journal = getJournal();
      const next = recordMissedSearch(journal, attempt.query);
      if (next === journal) return false;
      onRecord(next);
      return true;
    },
  };
}

export function exportMissedSearchJournal(journal, releaseVersion, now = new Date()) {
  return JSON.stringify({
    schemaVersion: 1,
    releaseVersion,
    exportedAt: now.toISOString(),
    entries: sanitizeMissedSearchJournal(journal).entries,
  }, null, 2);
}
