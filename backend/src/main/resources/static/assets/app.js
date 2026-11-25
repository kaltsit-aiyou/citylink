// querySelector 根据 CSS 选择器找到 HTML 元素，后续通过它读取输入或更新文字。
'use strict';
const $ = (selector) => document.querySelector(selector);
const SESSION_KEY = 'citylink.account.session';
const state = { mode: 'login', token: null, expiresAt: 0, profile: null, generation: 0, timer: null, busy: false };

function notice(target, message = '', kind = 'error') {
  const box = $(target);
  // textContent 把返回数据当作文字显示，避免把用户昵称解释为 HTML。
  box.textContent = message;
  box.dataset.kind = kind;
  box.hidden = !message;
}

function passwordVisible(visible) {
  $('#password').type = visible ? 'text' : 'password';
  $('#password-toggle').textContent = visible ? '隐藏' : '显示';
  $('#password-toggle').setAttribute('aria-pressed', String(visible));
}

function showAuth(mode = 'login', message = '', kind = 'error') {
  state.mode = mode === 'register' ? 'register' : 'login';
  const registering = state.mode === 'register';
  $('#welcome').hidden = false;
  $('#account').hidden = true;
  $('#header-account').hidden = true;
  $('#header-tag').hidden = false;
  $('#auth-title').textContent = registering ? '与城市，重新认识' : '欢迎回来';
  $('#auth-description').textContent = registering ? '创建账户，开启你的城邻云日常。' : '登录账户，继续你的城市生活。';
  $('#login-mode').setAttribute('aria-pressed', String(!registering));
  $('#register-mode').setAttribute('aria-pressed', String(registering));
  $('#username-help').textContent = registering ? '3～50 个字符，仅支持英文字母、数字和下划线。' : '用户名与昵称不同，请使用注册时的用户名。';
  $('#password-help').hidden = !registering;
  $('#confirm-group').hidden = !registering;
  $('#confirm-password').required = registering;
  $('#confirm-password').disabled = !registering;
  $('#password').autocomplete = registering ? 'new-password' : 'current-password';
  $('#password').value = '';
  $('#confirm-password').value = '';
  passwordVisible(false);
  $('#auth-submit').textContent = registering ? '创建我的账户' : '登录城邻云';
  $('#auth-bottom').textContent = registering ? '第一份美好，从认识你开始。' : '很高兴，再次与你相遇。';
  notice('#auth-notice', message, kind);
  history.replaceState(null, '', `#${state.mode}`);
}

function showAccount() {
  $('#welcome').hidden = true;
  $('#account').hidden = false;
  $('#header-account').hidden = false;
  $('#header-tag').hidden = true;
  history.replaceState(null, '', '#profile');
}

function clearSession(message = '', kind = 'info') {
  // generation 改变后，退出前发出的旧请求不能再覆盖当前页面。
  state.generation += 1;
  state.token = null;
  state.profile = null;
  state.expiresAt = 0;
  clearTimeout(state.timer);
  try { sessionStorage.removeItem(SESSION_KEY); } catch { /* 存储被浏览器禁用时仍可退出内存中的登录状态。 */ }
  $('#profile-form').reset();
  // 清掉上一个账户的显示资料，避免新账户加载失败时看见旧昵称。
  $('#greeting-name').textContent = '城邻';
  $('#profile-name').textContent = '正在加载';
  $('#profile-handle').textContent = '';
  $('#avatar-initial').textContent = '邻';
  $('#profile-fields').disabled = true;
  $('#avatar-image').hidden = true;
  $('#avatar-image').removeAttribute('src');
  $('#header-nickname').textContent = '';
  showAuth('login', message, kind);
}

function scheduleExpiry() {
  clearTimeout(state.timer);
  const remaining = state.expiresAt - Date.now();
  if (remaining <= 0) {
    clearSession('登录已过期，请重新登录。');
    return;
  }
  state.timer = setTimeout(() => clearSession('登录已过期，请重新登录。'), remaining);
}

function saveSession(login) {
  if (!login.accessToken || !Number.isFinite(login.expiresIn) || login.expiresIn <= 0 || login.expiresIn > 86400) {
    throw new Error('登录响应不完整，请重试。');
  }
  state.generation += 1;
  state.token = login.accessToken;
  state.expiresAt = Date.now() + login.expiresIn * 1000;
  // sessionStorage 只为当前标签页保存登录状态，刷新可继续；不保存密码。
  try { sessionStorage.setItem(SESSION_KEY, JSON.stringify({ token: state.token, expiresAt: state.expiresAt })); } catch { /* 存储不可用时本次登录仍有效，刷新后需重登。 */ }
  scheduleExpiry();
}

// async/await 表示等待网络响应；fetch 把 JSON 发送给真实后端。
// /api 是相对地址，页面和接口由同一个 Spring Boot 提供，因此不需要跨域配置。
async function api(path, { method = 'GET', body, authenticated = false } = {}) {
  const headers = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json; charset=UTF-8';
  if (authenticated) {
    if (!state.token || state.expiresAt <= Date.now()) {
      clearSession('登录已过期，请重新登录。');
      throw new Error('请先登录。');
    }
    headers.Authorization = `Bearer ${state.token}`;
  }
  const generation = state.generation;
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 12000);
  let response;
  let data;
  try {
    response = await fetch(path, {
      method, headers, signal: controller.signal, cache: 'no-store',
      // GET 不设置 body；其他请求把 JavaScript 对象转换成 JSON 字符串。
      ...(body !== undefined ? { body: JSON.stringify(body) } : {})
    });
    const text = await response.text();
    try { data = text ? JSON.parse(text) : null; } catch { data = null; }
  } catch (error) {
    throw new Error(error.name === 'AbortError' ? '请求超时，请稍后重试。' : '连接失败，请确认后端正在运行，再重试。');
  } finally {
    clearTimeout(timeout);
  }
  if (authenticated && generation !== state.generation) throw new Error('登录状态已变化。');
  if (!response.ok) {
    if (response.status === 401 && authenticated) clearSession('登录已失效，请重新登录。');
    const messages = {
      400: '填写内容不符合要求，请检查表单后重试。',
      401: authenticated ? '登录已失效，请重新登录。' : '用户名或密码不正确，请重新输入。',
      403: '当前账户没有权限执行此操作。',
      404: '未找到所需资料，请重新登录或稍后重试。',
      409: '这个用户名已经被使用，请换一个用户名。'
    };
    throw new Error(messages[response.status] || '服务暂时不可用，请稍后重试。');
  }
  if (!data) throw new Error('服务器返回了无法识别的数据，请稍后重试。');
  return data;
}

function safeAvatar(value) {
  if (!value) return '';
  try {
    const url = new URL(value);
    return ['http:', 'https:'].includes(url.protocol) ? url.href : '';
  } catch { return ''; }
}

function renderProfile(profile) {
  state.profile = profile;
  $('#greeting-name').textContent = profile.nickname;
  $('#header-nickname').textContent = profile.nickname;
  $('#profile-name').textContent = profile.nickname;
  $('#profile-handle').textContent = `@${profile.username}`;
  $('#account-username').value = profile.username;
  $('#nickname').value = profile.nickname;
  $('#avatar-url').value = profile.avatarUrl || '';
  $('#avatar-initial').textContent = Array.from(profile.nickname || profile.username)[0] || '邻';
  const img = $('#avatar-image');
  img.hidden = true;
  const avatar = safeAvatar(profile.avatarUrl);
  if (avatar) img.src = avatar;
  else img.removeAttribute('src');
}

async function loadProfile() {
  showAccount();
  const generation = state.generation;
  $('#profile-fields').disabled = true;
  $('#retry-profile').hidden = true;
  notice('#profile-notice', '正在读取你的资料…', 'info');
  try {
    const profile = await api('/api/users/me', { authenticated: true });
    if (generation !== state.generation) return;
    renderProfile(profile);
    $('#profile-fields').disabled = false;
    notice('#profile-notice');
  } catch (error) {
    if (generation !== state.generation) return;
    notice('#profile-notice', error.message);
    $('#retry-profile').hidden = false;
  }
}

function validateAuth(username, password) {
  if (!username || !password.trim()) return '请填写用户名和密码。';
  if (state.mode !== 'register') return '';
  if (!/^[A-Za-z0-9_]{3,50}$/.test(username)) return '用户名需要 3～50 个英文字母、数字或下划线。';
  if (password.length < 8 || password.length > 64) return '密码长度需要在 8～64 个字符之间。';
  if (new TextEncoder().encode(password).length > 72) return '密码过长：不能超过 72 个 UTF-8 字节，请适当缩短。';
  if (password !== $('#confirm-password').value) return '两次输入的密码不一致。';
  return '';
}

$('#auth-form').addEventListener('submit', async (event) => {
  // preventDefault 阻止表单刷新整页，由下面的 JavaScript 发送请求。
  event.preventDefault();
  if (state.busy) return;
  const username = $('#username').value.trim();
  const password = $('#password').value;
  const error = validateAuth(username, password);
  if (error) return notice('#auth-notice', error);
  const registering = state.mode === 'register';
  state.busy = true;
  $('#auth-fields').disabled = true;
  $('#login-mode').disabled = $('#register-mode').disabled = true;
  $('#auth-submit').textContent = registering ? '正在创建账户…' : '正在登录…';
  notice('#auth-notice');
  try {
    const result = await api(registering ? '/api/auth/register' : '/api/auth/login', { method: 'POST', body: { username, password } });
    if (registering) {
      showAuth('login', '注册成功！请输入密码登录你的新账户。', 'success');
    } else {
      saveSession(result);
      $('#password').value = '';
      await loadProfile();
    }
  } catch (error) {
    notice('#auth-notice', error.message);
  } finally {
    state.busy = false;
    $('#auth-fields').disabled = false;
    $('#login-mode').disabled = $('#register-mode').disabled = false;
    $('#auth-submit').textContent = state.mode === 'register' ? '创建我的账户' : '登录城邻云';
    // 先解除 fieldset 的禁用状态，输入框才能获得焦点。
    if (registering && state.mode === 'login') $('#password').focus();
  }
});

$('#profile-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const nickname = $('#nickname').value.trim();
  const avatarUrl = $('#avatar-url').value.trim();
  if (!nickname || nickname.length > 50) return notice('#profile-notice', '昵称不能为空，且不能超过 50 个字符。');
  if (avatarUrl && !safeAvatar(avatarUrl)) return notice('#profile-notice', '头像地址需要以 http:// 或 https:// 开头。');
  const generation = state.generation;
  $('#profile-fields').disabled = true;
  $('#save-profile').textContent = '正在保存…';
  notice('#profile-notice');
  try {
    const profile = await api('/api/users/me', { method: 'PUT', authenticated: true, body: { nickname, avatarUrl: avatarUrl || null } });
    if (generation !== state.generation) return;
    renderProfile(profile);
    notice('#profile-notice', '资料已保存。', 'success');
  } catch (error) {
    if (generation === state.generation) notice('#profile-notice', error.message);
  } finally {
    if (generation === state.generation) $('#profile-fields').disabled = false;
    $('#save-profile').textContent = '保存修改';
  }
});

$('#login-mode').addEventListener('click', () => showAuth('login'));
$('#register-mode').addEventListener('click', () => showAuth('register'));
$('#password-toggle').addEventListener('click', () => passwordVisible($('#password').type === 'password'));
$('#logout').addEventListener('click', () => clearSession('你已退出登录。'));
$('#retry-profile').addEventListener('click', loadProfile);
$('#reset-profile').addEventListener('click', () => { if (state.profile) renderProfile(state.profile); notice('#profile-notice'); });
$('#avatar-image').addEventListener('load', () => { $('#avatar-image').hidden = false; });
$('#avatar-image').addEventListener('error', () => { $('#avatar-image').hidden = true; });

function route() {
  // 保留“跳到主要内容”链接的浏览器默认行为。
  if (location.hash === '#main') return;
  if (state.busy) return;
  if (state.token) {
    showAccount();
  } else {
    showAuth(location.hash === '#register' ? 'register' : 'login');
  }
}
window.addEventListener('hashchange', route);
// 浏览器标签页休眠后恢复时再检查过期时间，避免定时器暂停造成界面滞后。
document.addEventListener('visibilitychange', () => { if (!document.hidden && state.token) scheduleExpiry(); });

async function start() {
  try {
    const saved = JSON.parse(sessionStorage.getItem(SESSION_KEY) || 'null');
    if (typeof saved?.token === 'string' && Number.isFinite(saved.expiresAt) && saved.expiresAt > Date.now() && saved.expiresAt - Date.now() <= 86400000) {
      state.token = saved.token;
      state.expiresAt = saved.expiresAt;
      scheduleExpiry();
      await loadProfile();
      return;
    }
    sessionStorage.removeItem(SESSION_KEY);
  } catch { /* 没有可用的本地登录记录时，正常显示登录页。 */ }
  route();
}
start();
