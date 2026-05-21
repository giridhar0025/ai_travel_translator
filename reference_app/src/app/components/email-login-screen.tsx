import { ArrowLeft, Mail, Lock } from 'lucide-react';
import { useState } from 'react';

interface EmailLoginScreenProps {
  onBack: () => void;
  onLogin: () => void;
  onForgotPassword: () => void;
  onSignUp: () => void;
}

export function EmailLoginScreen({ onBack, onLogin, onForgotPassword, onSignUp }: EmailLoginScreenProps) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  return (
    <div className="min-h-screen bg-[#F8FAFC] flex flex-col">
      {/* Header */}
      <div className="pt-6 px-6 pb-8">
        <button 
          onClick={onBack}
          className="p-2 -ml-2 rounded-lg hover:bg-white/50 transition-colors"
        >
          <ArrowLeft className="w-6 h-6 text-[#111827]" />
        </button>
        
        <div className="mt-8">
          <h1 className="text-2xl font-semibold text-[#111827] mb-2">
            Sign in with Email
          </h1>
          <p className="text-[#6B7280] text-base">
            Enter your credentials to continue
          </p>
        </div>
      </div>

      {/* Form */}
      <div className="flex-1 px-6">
        <div className="space-y-4">
          {/* Email Input */}
          <div>
            <label className="block text-sm text-[#111827] mb-2">Email</label>
            <div className="relative">
              <Mail className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-[#6B7280]" />
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="your@email.com"
                className="w-full bg-white border border-gray-200 rounded-xl pl-12 pr-4 py-3.5 text-[#111827] placeholder:text-[#6B7280] focus:outline-none focus:ring-2 focus:ring-[#FF6B4A] focus:border-transparent"
              />
            </div>
          </div>

          {/* Password Input */}
          <div>
            <label className="block text-sm text-[#111827] mb-2">Password</label>
            <div className="relative">
              <Lock className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-[#6B7280]" />
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="w-full bg-white border border-gray-200 rounded-xl pl-12 pr-4 py-3.5 text-[#111827] placeholder:text-[#6B7280] focus:outline-none focus:ring-2 focus:ring-[#FF6B4A] focus:border-transparent"
              />
            </div>
          </div>

          {/* Forgot Password */}
          <div className="flex justify-end">
            <button
              onClick={onForgotPassword}
              className="text-sm text-[#FF6B4A] hover:text-[#E85533] transition-colors"
            >
              Forgot password?
            </button>
          </div>

          {/* Login Button */}
          <button
            onClick={onLogin}
            className="w-full bg-[#FF6B4A] text-white rounded-xl px-6 py-4 shadow-sm hover:bg-[#E85533] transition-all active:scale-[0.98] mt-2"
          >
            Sign In
          </button>
        </div>
      </div>

      {/* Sign Up Link */}
      <div className="px-6 pb-8 text-center">
        <p className="text-sm text-[#6B7280]">
          Don't have an account?{' '}
          <button
            onClick={onSignUp}
            className="text-[#FF6B4A] hover:text-[#E85533] transition-colors"
          >
            Sign up
          </button>
        </p>
      </div>
    </div>
  );
}