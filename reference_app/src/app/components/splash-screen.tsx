import { Languages } from 'lucide-react';

interface SplashScreenProps {
  onComplete: () => void;
}

export function SplashScreen({ onComplete }: SplashScreenProps) {
  // Auto-advance after 2 seconds
  setTimeout(onComplete, 2000);

  return (
    <div className="fixed inset-0 bg-gradient-to-br from-[#FF6B4A] to-[#E85533] flex items-center justify-center">
      <div className="text-center animate-fade-in">
        <div className="mb-6 flex justify-center">
          <div className="bg-white/20 backdrop-blur-sm rounded-3xl p-6">
            <Languages className="w-20 h-20 text-white" strokeWidth={1.5} />
          </div>
        </div>
        <h1 className="text-white text-3xl font-semibold tracking-tight">
          Travel Translator AI
        </h1>
        <p className="text-white/80 mt-2">Speak the world</p>
      </div>
    </div>
  );
}