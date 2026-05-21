import { Mail, Chrome, Apple, Languages } from 'lucide-react';

interface LoginScreenProps {
  onGoogleLogin: () => void;
  onAppleLogin: () => void;
  onEmailLogin: () => void;
  onSkip: () => void;
}

export function LoginScreen({ onGoogleLogin, onAppleLogin, onEmailLogin, onSkip }: LoginScreenProps) {
  return (
    <div className="min-h-screen bg-[#F8FAFC] flex flex-col">
      {/* Header */}
      <div className="pt-16 pb-8 px-6 text-center">
        <div className="inline-flex items-center justify-center w-16 h-16 bg-gradient-to-br from-[#FF6B4A] to-[#E85533] rounded-2xl mb-4">
          <Languages className="w-8 h-8 text-white" strokeWidth={2} />
        </div>
        <h1 className="text-2xl font-semibold text-[#111827] mb-2">
          Welcome to Travel Translator
        </h1>
        <p className="text-[#6B7280] text-base">
          Connect with locals anywhere in the world
        </p>
      </div>

      {/* Login Options */}
      <div className="flex-1 px-6 pb-8">
        <div className="space-y-3 mb-6">
          {/* Google Login */}
          <button
            onClick={onGoogleLogin}
            className="w-full bg-white border border-gray-200 rounded-xl px-6 py-4 flex items-center justify-center gap-3 shadow-sm hover:shadow-md transition-shadow active:scale-[0.98]"
          >
            <Chrome className="w-5 h-5 text-[#111827]" />
            <span className="text-[#111827]">Continue with Google</span>
          </button>

          {/* Apple Login */}
          <button
            onClick={onAppleLogin}
            className="w-full bg-black rounded-xl px-6 py-4 flex items-center justify-center gap-3 shadow-sm hover:shadow-md transition-shadow active:scale-[0.98]"
          >
            <Apple className="w-5 h-5 text-white" fill="currentColor" />
            <span className="text-white">Continue with Apple</span>
          </button>

          {/* Email Login */}
          <button
            onClick={onEmailLogin}
            className="w-full bg-[#FF6B4A] rounded-xl px-6 py-4 flex items-center justify-center gap-3 shadow-sm hover:shadow-md transition-shadow active:scale-[0.98]"
          >
            <Mail className="w-5 h-5 text-white" />
            <span className="text-white">Continue with Email</span>
          </button>
        </div>

        {/* Skip Option */}
        <button
          onClick={onSkip}
          className="w-full text-[#6B7280] text-sm py-3 hover:text-[#111827] transition-colors"
        >
          Skip for now
        </button>
      </div>

      {/* Footer */}
      <div className="px-6 pb-8 text-center">
        <p className="text-xs text-[#6B7280]">
          By continuing, you agree to our Terms of Service and Privacy Policy
        </p>
      </div>
    </div>
  );
}