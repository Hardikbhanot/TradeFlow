import React, { createContext, useContext, useState, useEffect } from 'react';

export const THEMES = [
    {
        id: 'titanium',
        name: 'Luxe Titanium',
        desc: 'Sunset Amber & Matte Graphite (Recommended)',
        primaryColor: '#F59E0B',
        bgColor: '#0F1218',
        cardColor: '#161B24',
        accentTag: 'Warm Amber',
    },
    {
        id: 'emerald',
        name: 'Classic Emerald',
        desc: 'Original TradeFlow Palette & Deep Slate',
        primaryColor: '#10B981',
        bgColor: '#0B0F17',
        cardColor: '#0F172A',
        accentTag: 'Original',
    },
    {
        id: 'sapphire',
        name: 'Royal Sapphire',
        desc: 'Cobalt Blue & Deep Space Navy',
        primaryColor: '#3B82F6',
        bgColor: '#0B1120',
        cardColor: '#1E293B',
        accentTag: 'Cobalt',
    },
    {
        id: 'light',
        name: 'Daylight Pro',
        desc: 'Crisp High-Contrast Light Mode',
        primaryColor: '#D97706',
        bgColor: '#F8FAFC',
        cardColor: '#FFFFFF',
        accentTag: 'Light',
    },
];

const ThemeContext = createContext();

export function ThemeProvider({ children }) {
    const [theme, setTheme] = useState(() => {
        const saved = localStorage.getItem('tf_theme');
        if (saved && THEMES.some(t => t.id === saved)) {
            return saved;
        }
        // Legacy support: if previously saved as 'light', use 'light'
        if (saved === 'light') return 'light';
        return 'titanium'; // Default to Option B: Luxe Titanium & Sunset Amber
    });

    useEffect(() => {
        // Remove all previous theme classes
        document.body.classList.remove('theme-titanium', 'theme-emerald', 'theme-sapphire', 'theme-light', 'light-theme');
        
        // Add current theme class
        document.body.classList.add(`theme-${theme}`);
        if (theme === 'light') {
            document.body.classList.add('light-theme');
        }
        
        localStorage.setItem('tf_theme', theme);
    }, [theme]);

    const activeThemeObj = THEMES.find(t => t.id === theme) || THEMES[0];

    return (
        <ThemeContext.Provider value={{ theme, setTheme, themes: THEMES, activeThemeObj }}>
            {children}
        </ThemeContext.Provider>
    );
}

export function useTheme() {
    const context = useContext(ThemeContext);
    if (!context) {
        throw new Error('useTheme must be used within a ThemeProvider');
    }
    return context;
}
