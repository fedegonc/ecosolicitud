(() => {
    const iniciar = () => {
        const catalogo = document.getElementById('catalogo');
        if (!catalogo) return;
        const estilos = getComputedStyle(document.documentElement);
        catalogo.querySelectorAll('[data-token]').forEach(muestra => {
            muestra.textContent = estilos.getPropertyValue(muestra.dataset.token).trim();
        });
        const luminancia = token => {
            // Estos pares usan los colores opacos del tema, resueltos por el navegador.
            const muestra = document.createElement('span');
            muestra.style.color = `var(${token})`;
            catalogo.append(muestra);
            const color = getComputedStyle(muestra).color.match(/[\d.]+/g).map(Number);
            muestra.remove();
            const rgb = color.slice(0, 3).map(c => {
                c /= 255;
                return c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
            });
            return rgb[0] * 0.2126 + rgb[1] * 0.7152 + rgb[2] * 0.0722;
        };
        catalogo.querySelectorAll('[data-contraste]').forEach(muestra => {
            const [a, b] = muestra.dataset.contraste.split(',').map(luminancia);
            muestra.querySelector('output').textContent =
                `${((Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05)).toFixed(2)}:1`;
        });
    };
    iniciar();
    if (window.catalogoEventos) return;
    window.catalogoEventos = true;
    document.addEventListener('htmx:afterSwap', iniciar);
    document.addEventListener('submit', event => {
        const form = event.target;
        if (form.id !== 'catalogo-form') return;
        event.preventDefault();
        event.stopPropagation();
        const estado = document.getElementById('catalogo-estado');
        const boton = form.querySelector('[type=submit]');
        boton.disabled = true;
        boton.setAttribute('aria-busy', 'true');
        estado.textContent = estado.dataset.enviando;
        setTimeout(() => {
            boton.disabled = false;
            boton.removeAttribute('aria-busy');
            estado.textContent = estado.dataset.listo;
        }, 800);
    }, true);
    document.addEventListener('click', event => {
        const boton = event.target.closest('[data-accion-muestra]');
        if (!boton || !boton.closest('#catalogo')) return;
        const estado = document.getElementById('catalogo-estado');
        estado.textContent = `${boton.textContent.trim()}: ${estado.dataset.listo}`;
    });
})();
