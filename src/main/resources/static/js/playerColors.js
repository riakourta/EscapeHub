(() => {
    'use strict';

    const palette = Object.freeze({
        blue: Object.freeze({
            hex: '#38bdf8',
            background: 'rgba(14, 165, 233, 0.22)'
        }),
        pink: Object.freeze({
            hex: '#f472b6',
            background: 'rgba(236, 72, 153, 0.22)'
        }),
        purple: Object.freeze({
            hex: '#c084fc',
            background: 'rgba(168, 85, 247, 0.22)'
        }),
        green: Object.freeze({
            hex: '#34d399',
            background: 'rgba(16, 185, 129, 0.22)'
        }),
        red: Object.freeze({
            hex: '#f87171',
            background: 'rgba(239, 68, 68, 0.22)'
        }),
        yellow: Object.freeze({
            hex: '#facc15',
            background: 'rgba(234, 179, 8, 0.22)'
        })
    });

    function normalize(value) {
        if (typeof value !== 'string') return 'blue';

        const candidate = value.trim().toLowerCase();

        if (Object.prototype.hasOwnProperty.call(palette, candidate)) {
            return candidate;
        }

        return Object.keys(palette).find(
            name => palette[name].hex === candidate
        ) || 'blue';
    }

    function get(value) {
        return palette[normalize(value)];
    }

    function toHex(value) {
        return get(value).hex;
    }

    function applyCssVariables() {
        const root = document.documentElement;

        for (const [name, color] of Object.entries(palette)) {
            root.style.setProperty(`--player-${name}`, color.hex);
            root.style.setProperty(
                `--player-${name}-background`,
                color.background
            );
        }
    }

    window.PlayerColors = Object.freeze({
        palette,
        normalize,
        get,
        toHex
    });

    applyCssVariables();
})();