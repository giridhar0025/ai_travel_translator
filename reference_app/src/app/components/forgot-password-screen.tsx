import { ArrowLeft, Mail, CheckCircle2 } from 'lucide-react';
import { useState } from 'react';

interface ForgotPasswordScreenProps {
  onBack: () => void;
}

export function ForgotPasswordScreen({ onBack }: ForgotPasswordScreenProps) {
  const [email, setEmail] = useState('');
  const [sent, setSent] = useState(false);

  const handleReset = () => {
    setSent(true);
  };

  if (sent) {
    return (
      <div className="min-h-screen bg-[#F8FAFC] flex flex-col">
        <div className="pt-6 px-6 pb-8">
          <button 
            onClick={onBack}
            className="p-2 -ml-2 rounded-lg hover:bg-white/50 transition-colors"
          >
            <ArrowLeft className="w-6 h-6 text-[#111827]" />
          </button>
        </div>

        <div className="flex-1 flex items-center justify-center px-6">
          <div className="text-center max-w-sm">
            <div className="inline-flex items-center justify-center w-16 h-16 bg-[#22C55E]/10 rounded-full mb-6">
              <CheckCircle2 className="w-8 h-8 text-[#22C55E]" />
            </div>
            <h1 className="text-2xl font-semibold text-[#111827] mb-3">
              Check Your Email
            </h1>
            <p className="text-[#6B7280] mb-2">
              We've sent a password reset link to:
            </p>
            <p className="text-[#111827] font-medium mb-6">
              {email}
            </p>
            <button
              onClick={onBack}
              className="w-full bg-[#FF6B4A] text-white rounded-xl px-6 py-4 shadow-sm hover:bg-[#E85533] transition-all active:scale-[0.98]"
            >
              Back to Login
            </button>
          </div>
        </div>
      </div>
    );
  }

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
            Reset Password
          </h1>
          <p className="text-[#6B7280] text-base">
            Enter your email and we'll send you a reset link
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

          {/* Reset Button */}
          <button
            onClick={handleReset}
            className="w-full bg-[#FF6B4A] text-white rounded-xl px-6 py-4 shadow-sm hover:bg-[#E85533] transition-all active:scale-[0.98] mt-2"
          >
            Send Reset Link
          </button>
        </div>
      </div>
    </div>
  );
}
