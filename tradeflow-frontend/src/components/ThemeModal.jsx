import React from 'react';
import { useTheme } from '../context/ThemeContext';

export default function ThemeModal({ isOpen, onClose }) {
    const { theme, setTheme, themes } = useTheme();

    if (!isOpen) return null;

    return (
        <div className="modal-overlay" onClick={onClose} style={{ backdropFilter: 'blur(16px)', zIndex: 1200 }}>
            <div 
                className="card theme-picker-modal" 
                onClick={(e) => e.stopPropagation()}
                style={{ 
                    maxWidth: '560px', 
                    width: '92%', 
                    borderRadius: '20px',
                    border: '1px solid var(--border2)',
                    boxShadow: '0 25px 60px rgba(0, 0, 0, 0.6)',
                    padding: '2rem',
                    animation: 'fadeInUp 0.25s ease'
                }}
            >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
                    <div>
                        <div style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--text)' }}>Display & Color Themes</div>
                        <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)', marginTop: '4px' }}>
                            Choose your preferred visual aesthetic and color palette.
                        </div>
                    </div>
                    <button 
                        className="btn btn-ghost" 
                        onClick={onClose}
                        style={{ width: '36px', height: '36px', padding: 0, borderRadius: '50%', fontSize: '1rem' }}
                    >
                        ✕
                    </button>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1.5rem' }}>
                    {themes.map((t) => {
                        const isSelected = theme === t.id;
                        return (
                            <div
                                key={t.id}
                                onClick={() => setTheme(t.id)}
                                style={{
                                    border: isSelected ? `2px solid ${t.primaryColor}` : '1px solid var(--border)',
                                    borderRadius: '14px',
                                    padding: '1.25rem',
                                    cursor: 'pointer',
                                    background: isSelected ? 'var(--surface-hover)' : 'var(--surface2)',
                                    transition: 'all 0.2s ease',
                                    position: 'relative',
                                    overflow: 'hidden'
                                }}
                            >
                                {isSelected && (
                                    <div 
                                        style={{ 
                                            position: 'absolute', 
                                            top: '10px', 
                                            right: '10px', 
                                            width: '20px', 
                                            height: '20px', 
                                            borderRadius: '50%', 
                                            background: t.primaryColor, 
                                            color: '#000',
                                            display: 'flex',
                                            alignItems: 'center',
                                            justifyContent: 'center',
                                            fontSize: '0.75rem',
                                            fontWeight: 900
                                        }}
                                    >
                                        ✓
                                    </div>
                                )}

                                {/* Color Swatch Preview */}
                                <div style={{ display: 'flex', gap: '6px', marginBottom: '1rem', alignItems: 'center' }}>
                                    <div style={{ width: '22px', height: '22px', borderRadius: '6px', background: t.bgColor, border: '1px solid rgba(255,255,255,0.2)' }} title="Background" />
                                    <div style={{ width: '22px', height: '22px', borderRadius: '6px', background: t.cardColor, border: '1px solid rgba(255,255,255,0.2)' }} title="Surface" />
                                    <div style={{ width: '22px', height: '22px', borderRadius: '6px', background: t.primaryColor, boxShadow: `0 0 10px ${t.primaryColor}55` }} title="Accent" />
                                    <span 
                                        style={{ 
                                            marginLeft: 'auto', 
                                            fontSize: '0.68rem', 
                                            fontWeight: 700, 
                                            textTransform: 'uppercase', 
                                            padding: '2px 8px', 
                                            borderRadius: '12px',
                                            background: `${t.primaryColor}22`,
                                            color: t.primaryColor
                                        }}
                                    >
                                        {t.accentTag}
                                    </span>
                                </div>

                                <div style={{ fontWeight: 700, fontSize: '0.95rem', color: 'var(--text)', marginBottom: '4px' }}>
                                    {t.name}
                                </div>
                                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', lineHeight: '1.4' }}>
                                    {t.desc}
                                </div>
                            </div>
                        );
                    })}
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
                    <button 
                        className="btn btn-primary" 
                        onClick={onClose}
                        style={{ padding: '0.65rem 1.75rem', fontWeight: 700 }}
                    >
                        Done
                    </button>
                </div>
            </div>
        </div>
    );
}
