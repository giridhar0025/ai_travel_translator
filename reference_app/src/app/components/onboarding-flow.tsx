import { useState } from 'react';
import { MessageCircle, Volume2, Globe2, Mic, ChevronRight } from 'lucide-react';

interface OnboardingFlowProps {
  onComplete: () => void;
}

export function OnboardingFlow({ onComplete }: OnboardingFlowProps) {
  const [step, setStep] = useState(0);
  const [selectedLanguage, setSelectedLanguage] = useState('English');

  const languages = ['English', 'Hindi', 'Telugu', 'Thai'];

  const slides = [
    {
      icon: <MessageCircle className="w-16 h-16 text-[#FF6B4A]" strokeWidth={1.5} />,
      title: 'Speak Instantly',
      description: 'Communicate with locals in real-time using AI-powered translation'
    },
    {
      icon: <Volume2 className="w-16 h-16 text-[#FF6B4A]" strokeWidth={1.5} />,
      title: 'Translate Naturally',
      description: 'Get accurate, context-aware translations that sound natural'
    },
    {
      icon: <Globe2 className="w-16 h-16 text-[#FF6B4A]" strokeWidth={1.5} />,
      title: 'Play Audio to Locals',
      description: 'Let the app speak for you with native pronunciation'
    }
  ];

  const handleNext = () => {
    if (step < slides.length - 1) {
      setStep(step + 1);
    } else if (step === slides.length - 1) {
      setStep(slides.length); // Language selection
    } else if (step === slides.length) {
      setStep(slides.length + 1); // Microphone permission
    } else {
      onComplete();
    }
  };

  // Welcome Screen
  if (step === -1) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-[#FF6B4A] to-[#E85533] flex flex-col items-center justify-center px-6 text-center">
        <div className="mb-8">
          <MessageCircle className="w-24 h-24 text-white mx-auto mb-6" strokeWidth={1.5} />
          <h1 className="text-3xl font-semibold text-white mb-4">
            Welcome to Travel Translator
          </h1>
          <p className="text-white/90 text-lg">
            Break language barriers and connect with the world
          </p>
        </div>
        <button
          onClick={() => setStep(0)}
          className="bg-white text-[#FF6B4A] px-8 py-4 rounded-xl shadow-lg hover:shadow-xl transition-all active:scale-95"
        >
          Get Started
        </button>
      </div>
    );
  }

  // Intro Slides
  if (step < slides.length) {
    const slide = slides[step];
    return (
      <div className="min-h-screen bg-[#F8FAFC] flex flex-col">
        {/* Skip Button */}
        <div className="pt-6 px-6 flex justify-end">
          <button
            onClick={onComplete}
            className="text-[#6B7280] hover:text-[#111827] transition-colors"
          >
            Skip
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 flex flex-col items-center justify-center px-6 text-center">
          <div className="bg-[#FFF0ED] rounded-3xl p-8 mb-8">
            {slide.icon}
          </div>
          <h2 className="text-2xl font-semibold text-[#111827] mb-4">
            {slide.title}
          </h2>
          <p className="text-[#6B7280] text-base max-w-sm">
            {slide.description}
          </p>
        </div>

        {/* Progress & Next */}
        <div className="px-6 pb-8">
          <div className="flex items-center justify-center gap-2 mb-6">
            {slides.map((_, i) => (
              <div
                key={i}
                className={`h-2 rounded-full transition-all ${
                  i === step ? 'w-8 bg-[#FF6B4A]' : 'w-2 bg-gray-300'
                }`}
              />
            ))}
          </div>
          <button
            onClick={handleNext}
            className="w-full bg-[#FF6B4A] text-white rounded-xl px-6 py-4 flex items-center justify-center gap-2 shadow-sm hover:bg-[#E85533] transition-all active:scale-[0.98]"
          >
            <span>Next</span>
            <ChevronRight className="w-5 h-5" />
          </button>
        </div>
      </div>
    );
  }

  // Language Selection
  if (step === slides.length) {
    return (
      <div className="min-h-screen bg-[#F8FAFC] flex flex-col">
        <div className="pt-6 px-6 pb-8">
          <h2 className="text-2xl font-semibold text-[#111827] mb-2 mt-8">
            Choose Your Language
          </h2>
          <p className="text-[#6B7280] text-base">
            Select your primary language for translation
          </p>
        </div>

        <div className="flex-1 px-6">
          <div className="space-y-3">
            {languages.map((lang) => (
              <button
                key={lang}
                onClick={() => setSelectedLanguage(lang)}
                className={`w-full bg-white border-2 rounded-xl px-6 py-4 flex items-center justify-between transition-all ${
                  selectedLanguage === lang
                    ? 'border-[#FF6B4A] bg-[#FFF0ED]'
                    : 'border-gray-200 hover:border-gray-300'
                }`}
              >
                <span className={selectedLanguage === lang ? 'text-[#FF6B4A]' : 'text-[#111827]'}>
                  {lang}
                </span>
                {selectedLanguage === lang && (
                  <div className="w-5 h-5 bg-[#FF6B4A] rounded-full flex items-center justify-center">
                    <div className="w-2 h-2 bg-white rounded-full" />
                  </div>
                )}
              </button>
            ))}
          </div>
        </div>

        <div className="px-6 pb-8">
          <button
            onClick={handleNext}
            className="w-full bg-[#FF6B4A] text-white rounded-xl px-6 py-4 shadow-sm hover:bg-[#E85533] transition-all active:scale-[0.98]"
          >
            Continue
          </button>
        </div>
      </div>
    );
  }

  // Microphone Permission
  return (
    <div className="min-h-screen bg-[#F8FAFC] flex flex-col items-center justify-center px-6 text-center">
      <div className="bg-[#FFF0ED] rounded-3xl p-8 mb-8">
        <Mic className="w-16 h-16 text-[#FF6B4A] mx-auto" strokeWidth={1.5} />
      </div>
      <h2 className="text-2xl font-semibold text-[#111827] mb-4">
        Enable Microphone
      </h2>
      <p className="text-[#6B7280] text-base max-w-sm mb-8">
        Allow microphone access to use voice translation features
      </p>
      <button
        onClick={handleNext}
        className="w-full bg-[#FF6B4A] text-white rounded-xl px-6 py-4 shadow-sm hover:bg-[#E85533] transition-all active:scale-[0.98] mb-3"
      >
        Enable Microphone
      </button>
      <button
        onClick={onComplete}
        className="w-full text-[#6B7280] py-3 hover:text-[#111827] transition-colors"
      >
        Maybe Later
      </button>
    </div>
  );
}
