(() => {
    // Highlight da página ativa na topbar/sidebar
    const paginaAtual = window.location.pathname;

    document.querySelectorAll(".web-topbar-nav a, .web-sidebar a").forEach(link => {
        const href = link.getAttribute("href");
        if (href && paginaAtual.startsWith(href) && href !== "/") {
            link.classList.add("ativo", "btn-link-active");
        }
    });

    const sidebar = document.querySelector(".web-sidebar");
    const menuBtn = document.getElementById("menuToggle");
    if (menuBtn && sidebar) {
        menuBtn.addEventListener("click", () => {
            sidebar.classList.toggle("open");
        });
    }

    // Fechar sidebar ao clicar fora (mobile)
    document.addEventListener("click", (e) => {
        if (sidebar && sidebar.classList.contains("open")) {
            if (!sidebar.contains(e.target) && !menuBtn?.contains(e.target)) {
                sidebar.classList.remove("open");
            }
        }
    });

    window.addEventListener("resize", () => {
        if (!sidebar) {
            return;
        }
        if (window.innerWidth > 900) {
            sidebar.classList.remove("open");
        }
    });
})();
