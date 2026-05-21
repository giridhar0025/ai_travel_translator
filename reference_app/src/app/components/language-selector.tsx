import { Check, Globe2 } from 'lucide-react';
import { useState } from 'react';

interface LanguageSelectorProps {
  sourceLanguage: string;
  targetLanguage: string;
  onLanguageChange: (source: string, target: string) => void;
}

export function LanguageSelector({ 
  sourceLanguage, 
  targetLanguage, 
  onLanguageChange 
}: LanguageSelectorProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [mode, setMode] = useState<'source' | 'target' | null>(null);

  const languages = [
    { code: 'en', name: 'English', flag: '🇬🇧' },
    { code: 'hi', name: 'Hindi', flag: '🇮🇳' },
    { code: 'te', name: 'Telugu', flag: '🇮🇳' },
    { code: 'th', name: 'Thai', flag: '🇹🇭' }
  ];

  const handleLanguageSelect = (language: string) => {
    if (mode === 'source') {
      onLanguageChange(language, targetLanguage);
    } else if (mode === 'target') {
      onLanguageChange(sourceLanguage, language);
    }
    setIsOpen(false);
    setMode(null);
  };

  const openSourceSelector = () => {
    setMode('source');
    setIsOpen(true);
  };

  const openTargetSelector = () => {
    setMode('target');
    setIsOpen(true);
  };

  const swapLanguages = () => {
    onLanguageChange(targetLanguage, sourceLanguage);
  };

  return (
    <>
      {/* Language Bar */}
      <div className="bg-white border-b border-gray-200 px-4 py-3">
        <div className="flex items-center gap-2">
          <button
            onClick={openSourceSelector}
            className="flex-1 bg-[#F8FAFC] border border-gray-200 rounded-lg px-4 py-2.5 flex items-center justify-between hover:border-[#FF6B4A] transition-colors"
          >
            <div className="flex items-center gap-2">
              <Globe2 className="w-4 h-4 text-[#6B7280]" />
              <span className="text-sm text-[#111827]">{sourceLanguage}</span>
            </div>
          </button>

          <button
            onClick={swapLanguages}
            className="p-2.5 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <svg
              className="w-5 h-5 text-[#6B7280]"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M7 16V4m0 0L3 8m4-4l4 4m6 0v12m0 0l4-4m-4 4l-4-4"
              />
            </svg>
          </button>

          <button
            onClick={openTargetSelector}
            className="flex-1 bg-[#F8FAFC] border border-gray-200 rounded-lg px-4 py-2.5 flex items-center justify-between hover:border-[#FF6B4A] transition-colors"
          >
            <div className="flex items-center gap-2">
              <Globe2 className="w-4 h-4 text-[#6B7280]" />
              <span className="text-sm text-[#111827]">{targetLanguage}</span>
            </div>
          </button>
        </div>
      </div>

      {/* Language Selection Modal */}
      {isOpen && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-end sm:items-center justify-center">
          <div className="bg-white rounded-t-3xl sm:rounded-3xl w-full sm:max-w-md max-h-[80vh] overflow-hidden">
            <div className="px-6 py-4 border-b border-gray-200">
              <h3 className="text-lg font-semibold text-[#111827]">
                Select {mode === 'source' ? 'Source' : 'Target'} Language
              </h3>
            </div>
            
            <div className="overflow-y-auto max-h-96">
              {languages.map((lang) => {
                const isSelected = mode === 'source' 
                  ? lang.name === sourceLanguage 
                  : lang.name === targetLanguage;
                
                return (
                  <button
                    key={lang.code}
                    onClick={() => handleLanguageSelect(lang.name)}
                    className="w-full px-6 py-4 flex items-center justify-between hover:bg-[#F8FAFC] transition-colors border-b border-gray-100 last:border-0"
                  >
                    <div className="flex items-center gap-3">
                      <span className="text-2xl">{lang.flag}</span>
                      <span className={`text-base ${isSelected ? 'text-[#FF6B4A] font-medium' : 'text-[#111827]'}`}>
                        {lang.name}
                      </span>
                    </div>
                    {isSelected && (
                      <Check className="w-5 h-5 text-[#FF6B4A]" strokeWidth={2.5} />
                    )}
                  </button>
                );
              })}
            </div>

            <div className="px-6 py-4 border-t border-gray-200">
              <button
                onClick={() => {
                  setIsOpen(false);
                  setMode(null);
                }}
                className="w-full bg-[#F8FAFC] text-[#111827] rounded-xl px-6 py-3 hover:bg-gray-200 transition-colors"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
