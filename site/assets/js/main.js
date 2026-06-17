/* =====================================================================
   Open Roleplay — site interactions
   No dependencies. Progressive enhancement only.
   ===================================================================== */
(function () {
  "use strict";

  /* ---- Theme: light default, dark toggle, persisted -------------- */
  var root = document.documentElement;
  var stored = null;
  try { stored = localStorage.getItem("or-theme"); } catch (e) {}
  if (stored === "dark" || stored === "light") {
    root.setAttribute("data-theme", stored);
  } else if (window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches) {
    root.setAttribute("data-theme", "dark");
  }

  function bindTheme() {
    var toggles = document.querySelectorAll("[data-theme-toggle]");
    toggles.forEach(function (btn) {
      btn.addEventListener("click", function () {
        var next = root.getAttribute("data-theme") === "dark" ? "light" : "dark";
        root.setAttribute("data-theme", next);
        try { localStorage.setItem("or-theme", next); } catch (e) {}
      });
    });
  }

  /* ---- Sticky nav shadow on scroll ------------------------------- */
  function bindNav() {
    var nav = document.querySelector(".nav");
    if (!nav) return;
    var onScroll = function () { nav.classList.toggle("scrolled", window.scrollY > 8); };
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
  }

  /* ---- Mobile menu ----------------------------------------------- */
  function bindBurger() {
    var burger = document.querySelector("[data-burger]");
    var menu = document.querySelector(".mobile-menu");
    if (!burger || !menu) return;
    burger.addEventListener("click", function () {
      var open = menu.classList.toggle("open");
      burger.setAttribute("aria-expanded", open ? "true" : "false");
    });
    menu.querySelectorAll("a").forEach(function (a) {
      a.addEventListener("click", function () {
        menu.classList.remove("open");
        burger.setAttribute("aria-expanded", "false");
      });
    });
  }

  /* ---- Copy-to-clipboard buttons --------------------------------- */
  var COPY_ICON = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>';
  var OK_ICON = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>';
  var ARROW_ICON = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>';
  var REPO_URL = "https://github.com/giovyx90/open-roleplay";
  var RELEASE_DOWNLOAD_URL = REPO_URL + "/releases/latest/download/";

  var MODULE_ICONS = {
    weapons: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><line x1="12" y1="2" x2="12" y2="6"/><line x1="12" y1="18" x2="12" y2="22"/><line x1="2" y1="12" x2="6" y2="12"/><line x1="18" y1="12" x2="22" y2="12"/><circle cx="12" cy="12" r="2.4"/></svg>',
    access: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/><circle cx="12" cy="16" r="1.4"/></svg>',
    cosmetics: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3l1.8 4.3L18 9l-4.2 1.7L12 15l-1.8-4.3L6 9l4.2-1.7L12 3z"/><path d="M18.5 14l.9 2.1 2.1.9-2.1.9-.9 2.1-.9-2.1-2.1-.9 2.1-.9.9-2.1z"/></svg>',
    vending: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="5" y="2" width="14" height="20" rx="2"/><line x1="9" y1="6" x2="9" y2="9"/><line x1="9" y1="12" x2="9" y2="13"/><line x1="13" y1="6" x2="13" y2="9"/><rect x="8" y="17" width="8" height="2.5" rx="0.6"/></svg>',
    companies: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="6" width="16" height="15" rx="2"/><path d="M9 6V4h6v2"/><path d="M8 11h2M14 11h2M8 15h2M14 15h2"/></svg>',
    fdo: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2 4 5v6c0 5 3.5 8.6 8 10 4.5-1.4 8-5 8-10V5l-8-3z"/><path d="M12 7.8l1.2 2.5 2.7.3-2 1.9.6 2.7-2.5-1.4-2.5 1.4.6-2.7-2-1.9 2.7-.3z"/></svg>',
    crime: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 11l9-7 9 7"/><path d="M5 10v10h14V10"/><circle cx="9.5" cy="14.5" r="1.6"/><circle cx="14.5" cy="14.5" r="1.6"/><path d="M11.1 14.5h1.8"/></svg>',
    core: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 2 7 12 12 22 7 12 2"/><polyline points="2 17 12 22 22 17"/><polyline points="2 12 12 17 22 12"/></svg>',
    api: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 18l6-6-6-6"/><path d="M8 6l-6 6 6 6"/><line x1="13.5" y1="4" x2="10.5" y2="20"/></svg>',
    gestionale: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="3" y1="9" x2="21" y2="9"/><line x1="9" y1="21" x2="9" y2="9"/></svg>',
    jobs: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.5 5.5a3.5 3.5 0 0 0-4.6 4.6L3 17l4 4 6.9-6.9a3.5 3.5 0 0 0 4.6-4.6l-2.3 2.3-2.1-2.1z"/><path d="M14.5 9.5l4.5-4.5"/></svg>'
  };

  function esc(value) {
    var div = document.createElement("div");
    div.textContent = value == null ? "" : String(value);
    return div.innerHTML;
  }

  function moduleWord(count) {
    return {
      0: "Zero",
      1: "Un",
      2: "Due",
      3: "Tre",
      4: "Quattro",
      5: "Cinque",
      6: "Sei",
      7: "Sette",
      8: "Otto",
      9: "Nove",
      10: "Dieci",
      11: "Undici"
    }[count] || String(count);
  }

  function onModulePage() {
    return window.location.pathname.indexOf("/moduli/") !== -1;
  }

  function moduleHref(mod) {
    return (onModulePage() ? "" : "moduli/") + mod.page;
  }

  function sourceHref(mod) {
    return REPO_URL + "/tree/main/" + mod.sourceDir;
  }

  function countFallbackModules() {
    var grid = document.querySelector("[data-module-grid]") || document.querySelector(".modules-grid");
    return grid ? grid.querySelectorAll(".module-card").length : 0;
  }

  function setTextAll(selector, value) {
    document.querySelectorAll(selector).forEach(function (el) {
      el.textContent = String(value);
    });
  }

  function syncModuleCounts(modules) {
    var total = modules.length || countFallbackModules();
    var paperTotal = modules.length
      ? modules.filter(function (mod) { return mod.paperPlugin; }).length
      : total;
    var word = moduleWord(total);
    setTextAll("[data-module-total]", total);
    setTextAll("[data-module-total-word]", word);
    setTextAll("[data-module-total-word-lower]", word.toLowerCase());
    setTextAll("[data-paper-plugin-total]", paperTotal);
  }

  function renderModuleGrid(modules) {
    var grid = document.querySelector("[data-module-grid]");
    if (!grid || !modules.length) return;
    grid.innerHTML = modules.map(function (mod, i) {
      var delay = i % 3;
      var delayAttr = delay ? ' data-d="' + delay + '"' : "";
      var badgeClass = "badge" + (mod.statusClass ? " " + esc(mod.statusClass) : "");
      var tags = (mod.tags || []).map(function (tag) {
        return "<span>" + esc(tag) + "</span>";
      }).join("");
      return '<article class="module-card reveal"' + delayAttr + ">" +
        '<div class="module-card__icon">' + (MODULE_ICONS[mod.icon] || MODULE_ICONS.core) + "</div>" +
        '<div class="module-card__head"><div><h3>' + esc(mod.name) + '</h3><div class="id">' + esc(mod.id) + '</div></div><span class="' + badgeClass + '">' + esc(mod.status) + "</span></div>" +
        "<p>" + esc(mod.summary) + "</p>" +
        '<div class="tags">' + tags + "</div>" +
        '<div class="module-card__foot">' +
          '<a href="' + esc(moduleHref(mod)) + '">Scopri di più ' + ARROW_ICON + "</a>" +
          '<a class="sec" href="' + esc(sourceHref(mod)) + '" target="_blank" rel="noopener">Sorgente</a>' +
        "</div>" +
      "</article>";
    }).join("");
  }

  function renderDownloadGrid(modules) {
    var grid = document.querySelector("[data-download-grid]");
    if (!grid || !modules.length) return;
    grid.innerHTML = modules.map(function (mod, i) {
      var delay = i % 3;
      var delayAttr = delay ? ' data-d="' + delay + '"' : "";
      // JAR modules link to the release download; non-JAR modules (es. il
      // gestionale web) usano un override `download` o ripiegano sul sorgente.
      var primaryHref = mod.jar ? (RELEASE_DOWNLOAD_URL + mod.jar)
        : (mod.download ? mod.download.href : sourceHref(mod));
      var primaryLabel = mod.jar ? (mod.jarLabel || "Scarica JAR")
        : (mod.download ? mod.download.label : "Sorgente");
      var primaryExternal = !!(mod.jar || mod.download);
      return '<div class="dl-card reveal"' + delayAttr + ">" +
        '<div class="top"><div class="ic">' + (MODULE_ICONS[mod.icon] || MODULE_ICONS.core) + "</div><div><h4>" + esc(mod.name) + '</h4><div class="id">' + esc(mod.id) + "</div></div></div>" +
        '<div class="actions">' +
          '<a class="btn btn-primary btn-sm" href="' + esc(primaryHref) + '"' + (primaryExternal ? ' target="_blank" rel="noopener"' : "") + ">" + esc(primaryLabel) + "</a>" +
          '<a class="btn btn-outline btn-sm" href="' + esc(moduleHref(mod)) + '">Dettagli</a>' +
        "</div>" +
      "</div>";
    }).join("");
  }

  function renderModuleNav(modules) {
    if (!modules.length) return;
    document.querySelectorAll("[data-module-nav]").forEach(function (list) {
      list.innerHTML = modules.map(function (mod) {
        return '<li><a href="' + esc(moduleHref(mod)) + '">' + esc(mod.name) + "</a></li>";
      }).join("");
    });
  }

  function renderModuleData() {
    var modules = Array.isArray(window.OpenRoleplayModules) ? window.OpenRoleplayModules : [];
    syncModuleCounts(modules);
    renderModuleGrid(modules);
    renderDownloadGrid(modules);
    renderModuleNav(modules);
  }

  function bindCopy() {
    document.querySelectorAll("[data-copy]").forEach(function (btn) {
      btn.addEventListener("click", function () {
        var sel = btn.getAttribute("data-copy");
        var text = "";
        if (sel) {
          var target = document.querySelector(sel);
          if (target) text = target.innerText;
        } else if (btn.getAttribute("data-copy-text")) {
          text = btn.getAttribute("data-copy-text");
        }
        var done = function () {
          var label = btn.querySelector(".copy-label");
          btn.classList.add("copied");
          btn.querySelector("svg").outerHTML = OK_ICON;
          if (label) label.textContent = "Copiato";
          setTimeout(function () {
            btn.classList.remove("copied");
            var s = btn.querySelector("svg");
            if (s) s.outerHTML = COPY_ICON;
            if (label) label.textContent = "Copia";
          }, 1800);
        };
        if (navigator.clipboard && navigator.clipboard.writeText) {
          navigator.clipboard.writeText(text.trim()).then(done).catch(function () {});
        } else {
          var ta = document.createElement("textarea");
          ta.value = text.trim(); document.body.appendChild(ta); ta.select();
          try { document.execCommand("copy"); done(); } catch (e) {}
          document.body.removeChild(ta);
        }
      });
    });
  }

  /* ---- Scroll reveal --------------------------------------------- */
  function bindReveal() {
    var els = document.querySelectorAll(".reveal");
    if (!("IntersectionObserver" in window) || !els.length) {
      els.forEach(function (el) { el.classList.add("is-visible"); });
      return;
    }
    var io = new IntersectionObserver(function (entries) {
      entries.forEach(function (en) {
        if (en.isIntersecting) { en.target.classList.add("is-visible"); io.unobserve(en.target); }
      });
    }, { threshold: 0.12, rootMargin: "0px 0px -8% 0px" });
    els.forEach(function (el) { io.observe(el); });
  }

  /* ---- Pointer-tracked glow on module cards ---------------------- */
  function bindCardGlow() {
    document.querySelectorAll(".module-card").forEach(function (card) {
      card.addEventListener("pointermove", function (e) {
        var r = card.getBoundingClientRect();
        card.style.setProperty("--mx", (e.clientX - r.left) + "px");
        card.style.setProperty("--my", (e.clientY - r.top) + "px");
      });
    });
  }

  /* ---- Active section in nav (landing) --------------------------- */
  function bindScrollSpy() {
    var links = document.querySelectorAll(".nav-links a[href^='#']");
    if (!links.length || !("IntersectionObserver" in window)) return;
    var map = {};
    links.forEach(function (l) { map[l.getAttribute("href").slice(1)] = l; });
    var spy = new IntersectionObserver(function (entries) {
      entries.forEach(function (en) {
        var link = map[en.target.id];
        if (link && en.isIntersecting) {
          links.forEach(function (l) { l.classList.remove("active"); });
          link.classList.add("active");
        }
      });
    }, { rootMargin: "-45% 0px -50% 0px" });
    Object.keys(map).forEach(function (id) {
      var sec = document.getElementById(id);
      if (sec) spy.observe(sec);
    });
  }

  /* ---- Footer year ----------------------------------------------- */
  function setYear() {
    document.querySelectorAll("[data-year]").forEach(function (el) {
      el.textContent = new Date().getFullYear();
    });
  }

  function init() {
    renderModuleData();
    bindTheme(); bindNav(); bindBurger(); bindCopy();
    bindReveal(); bindCardGlow(); bindScrollSpy(); setYear();
  }
  if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", init);
  else init();
})();
