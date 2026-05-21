import { useState, useEffect } from 'react';
import { ArrowLeft, Mic, X } from 'lucide-react';

interface VoiceInputScreenProps {
  onBack: () => void;
  onTranslate: (text: string) => void;
}

export function VoiceInputScreen({ onBack, onTranslate }: VoiceInputScreenProps) {
  const [isRecording, setIsRecording] = useState(true);
  const [transcript, setTranscript] = useState('');
  const [waveform, setWaveform] = useState([0.3, 0.5, 0.7, 0.4, 0.6, 0.8, 0.5, 0.3]);

  // Simulate waveform animation
  useEffect(() => {
    if (isRecording) {
      const interval = setInterval(() => {
        setWaveform(prev => prev.map(() => Math.random() * 0.8 + 0.2));
      }, 100);
      return () => clearInterval(interval);
    }
  }, [isRecording]);

  // Simulate live transcription
  useEffect(() => {
    if (isRecording) {
      const timeout = setTimeout(() => {
        setTranscript('Hello, can you help me find a restaurant nearby?');
        setIsRecording(false);
      }, 2000);
      return () => clearTimeout(timeout);
    }
  }, [isRecording]);

  const handleTranslate = () => {
    onTranslate(transcript);
    onBack();
  };

  return (
    <div className="fixed inset-0 bg-[#F8FAFC] z-50 flex flex-col">
      {/* Header */}
      <div className="pt-6 px-6 pb-4 flex items-center justify-between">
        <button
          onClick={onBack}
          className="p-2 -ml-2 rounded-lg hover:bg-white/50 transition-colors"
        >
          <ArrowLeft className="w-6 h-6 text-[#111827]" />
        </button>
        <h2 className="text-lg font-semibold text-[#111827]">Voice Input</h2>
        <div className="w-10" />
      </div>

      {/* Recording Visual */}
      <div className="flex-1 flex flex-col items-center justify-center px-6">
        {isRecording ? (
          <>
            <div className="relative mb-8">
              <div className="absolute inset-0 bg-[#FF6B4A]/20 rounded-full animate-ping" />
              <div className="relative bg-gradient-to-br from-[#FF6B4A] to-[#E85533] rounded-full p-8">
                <Mic className="w-16 h-16 text-white" strokeWidth={1.5} />
              </div>
            </div>
            
            {/* Waveform */}
            <div className="flex items-center gap-1.5 h-16 mb-6">
              {waveform.map((height, i) => (
                <div
                  key={i}
                  className="w-1 bg-[#FF6B4A] rounded-full transition-all duration-100"
                  style={{ height: `${height * 100}%` }}
                />
              ))}
            </div>

            <p className="text-[#111827] text-lg mb-2">Listening...</p>
            <p className="text-[#6B7280] text-sm">Speak clearly into your device</p>
          </>
        ) : (
          <>
            <div className="bg-[#22C55E]/10 rounded-full p-8 mb-6">
              <Mic className="w-16 h-16 text-[#22C55E]" strokeWidth={1.5} />
            </div>
            
            <p className="text-[#111827] text-lg mb-6 text-center">
              Recording complete
            </p>
            
            {/* Editable Transcript */}
            <div className="w-full bg-white rounded-xl p-4 border border-gray-200 mb-6">
              <label className="block text-sm text-[#6B7280] mb-2">Transcript</label>
              <textarea
                value={transcript}
                onChange={(e) => setTranscript(e.target.value)}
                className="w-full text-[#111827] focus:outline-none resize-none min-h-[80px]"
                placeholder="Your speech will appear here..."
              />
            </div>
          </>
        )}
      </div>

      {/* Actions */}
      <div className="px-6 pb-8 space-y-3">
        {isRecording ? (
          <button
            onClick={() => setIsRecording(false)}
            className="w-full bg-red-500 text-white rounded-xl px-6 py-4 flex items-center justify-center gap-2 shadow-sm hover:bg-red-600 transition-all active:scale-[0.98]"
          >
            <X className="w-5 h-5" />
            <span>Stop Recording</span>
          </button>
        ) : (
          <>
            <button
              onClick={handleTranslate}
              className="w-full bg-[#FF6B4A] text-white rounded-xl px-6 py-4 shadow-sm hover:bg-[#E85533] transition-all active:scale-[0.98]"
              disabled={!transcript.trim()}
            >
              Translate
            </button>
            <button
              onClick={() => {
                setTranscript('');
                setIsRecording(true);
              }}
              className="w-full bg-white border border-gray-200 text-[#111827] rounded-xl px-6 py-4 hover:bg-gray-50 transition-all active:scale-[0.98]"
            >
              Record Again
            </button>
          </>
        )}
      </div>
    </div>
  );
}
