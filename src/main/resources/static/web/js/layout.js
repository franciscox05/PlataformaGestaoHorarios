(() => {
    const atualizarAlturaTopbar = () => {
        const topbar = document.querySelector(".web-topbar");
        if (!topbar) {
            return;
        }

        const altura = Math.ceil(topbar.getBoundingClientRect().height);
        document.documentElement.style.setProperty("--topbar-height", `${altura}px`);
    };

    window.addEventListener("load", atualizarAlturaTopbar);
    window.addEventListener("resize", atualizarAlturaTopbar);
})();
