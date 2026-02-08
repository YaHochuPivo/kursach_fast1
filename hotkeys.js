(function(){
  function on(el, ev, fn){ el && el.addEventListener(ev, fn); }
  function $(sel){ return document.querySelector(sel); }
  function go(url){ if (url) window.location.href = url; }
  function openNewProperty(){ if (typeof openPropertyModal === 'function') openPropertyModal(null); }
  function showHelp(){
    const msg = [
      'Горячие клавиши:',
      'g h  — Главная',
      'g p  — Каталог «Недвижимость»',
      'g m  — Мои объявления',
      'g r  — Отчеты',
      '/    — Фокус на поиске (на главной)',
      'n    — Новое объявление (где доступно)',
      's    — Открыть настройки профиля (если открыт профиль)',
      '?    — Показать это окно'
    ].join('\n');
    alert(msg);
  }

  on(document, 'keydown', function(e){
    if (e.target && ['INPUT','TEXTAREA','SELECT'].includes(e.target.tagName)) return; // не перехватывать ввод
    // последовательности типа g h
    if (e.key === 'g'){ window.__hotkeysSeq = 'g'; setTimeout(()=>{ window.__hotkeysSeq = null; }, 1000); return; }
    if (window.__hotkeysSeq === 'g'){
      if (e.key === 'h'){ go('/'); e.preventDefault(); return; }
      if (e.key === 'p'){ go('/properties'); e.preventDefault(); return; }
      if (e.key === 'm'){ go('/user/properties'); e.preventDefault(); return; }
      if (e.key === 'r'){ go('/reports'); e.preventDefault(); return; }
    }
    if (e.key === '/'){ const el = $("form.search input[name='q']"); if (el){ el.focus(); e.preventDefault(); } return; }
    if (e.key === 'n'){ openNewProperty(); e.preventDefault(); return; }
    if (e.key === 's'){ if (location.pathname.startsWith('/user/profile')){ const el = document.getElementById('settings-theme'); if (el){ el.focus(); } e.preventDefault(); } return; }
    if (e.key === '?'){ showHelp(); e.preventDefault(); return; }
  });
})();
