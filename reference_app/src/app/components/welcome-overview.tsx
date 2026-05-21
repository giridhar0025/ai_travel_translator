import { Languages, ArrowRight } from 'lucide-react';

interface WelcomeOverviewProps {
  onStart: () => void;
}

export function WelcomeOverview({ onStart }: WelcomeOverviewProps) {
  return (
    <div className="min-h-screen bg-gradient-to-br from-[#FF6B4A] to-[#E85533] flex flex-col items-center justify-center px-6 text-center">
      <div className="max-w-md">
        <div className="mb-8">
          <div className="inline-flex items-center justify-center w-24 h-24 bg-white/20 backdrop-blur-sm rounded-3xl mb-6">
            <Languages className="w-12 h-12 text-white" strokeWidth={1.5} />
          </div>
          <h1 className="text-4xl font-semibold text-white mb-4">
            Travel Translator AI
          </h1>
          <p className="text-white/90 text-lg mb-8">
            Complete Mobile App UI Design
          </p>
        </div>

        <div className="bg-white/10 backdrop-blur-sm rounded-2xl p-6 mb-8 text-left">
          <h2 className="text-white text-lg font-medium mb-4">Includes:</h2>
          <ul className="space-y-2 text-white/90 text-sm">
            <li>✓ Splash Screen</li>
            <li>✓ Complete Authentication Flow</li>
            <li>✓ Onboarding Experience</li>
            <li>✓ Chat Translation Interface</li>
            <li>✓ Voice Input System</li>
            <li>✓ Quick Phrases Library</li>
            <li>✓ Conversation History</li>
            <li>✓ Settings & Preferences</li>
            <li>✓ Language Selector</li>
            <li>✓ Audio Playback Controls</li>
          </ul>
        </div>

        <button
          onClick={onStart}
          className="w-full bg-white text-[#FF6B4A] px-8 py-4 rounded-xl shadow-lg hover:shadow-xl transition-all active:scale-95 flex items-center justify-center gap-2 font-medium"
        >
          <span>Explore All Screens</span>
          <ArrowRight className="w-5 h-5" />
        </button>

        <p className="text-white/70 text-xs mt-6">
          Navigate through the app to see all screens in action
        </p>
      </div>
    </div>
  );
}