import { Play, Pause, Gauge } from 'lucide-react';
import { useState } from 'react';

interface AudioPlayerProps {
  text: string;
  onPlay?: () => void;
}

export function AudioPlayer({ text, onPlay }: AudioPlayerProps) {
  const [isPlaying, setIsPlaying] = useState(false);
  const [isSlowMode, setIsSlowMode] = useState(false);

  const handlePlay = () => {
    setIsPlaying(!isPlaying);
    onPlay?.();
    
    // Simulate playback
    if (!isPlaying) {
      setTimeout(() => {
        setIsPlaying(false);
      }, 2000);
    }
  };

  return (
    <div className="bg-white rounded-2xl p-6 shadow-lg border border-gray-100">
      <div className="mb-6">
        <p className="text-2xl text-[#111827] mb-2">{text}</p>
        <p className="text-[#6B7280] text-sm italic">
          Pronunciation guide will appear here
        </p>
      </div>

      {/* Waveform Visualization */}
      <div className="flex items-center justify-center gap-1 h-20 mb-6 bg-[#F8FAFC] rounded-xl px-4">
        {[...Array(40)].map((_, i) => (
          <div
            key={i}
            className={`flex-1 rounded-full transition-all ${
              isPlaying ? 'bg-[#FF6B4A] animate-pulse' : 'bg-gray-300'
            }`}
            style={{
              height: isPlaying
                ? `${Math.random() * 60 + 20}%`
                : '20%',
              animationDelay: `${i * 0.05}s`
            }}
          />
        ))}
      </div>

      {/* Controls */}
      <div className="flex items-center gap-3">
        <button
          onClick={handlePlay}
          className="flex-1 bg-gradient-to-r from-[#FF6B4A] to-[#E85533] text-white rounded-xl px-6 py-4 flex items-center justify-center gap-3 shadow-sm hover:shadow-md transition-all active:scale-[0.98]"
        >
          {isPlaying ? (
            <>
              <Pause className="w-6 h-6" fill="currentColor" />
              <span>Pause</span>
            </>
          ) : (
            <>
              <Play className="w-6 h-6" fill="currentColor" />
              <span>Play Audio</span>
            </>
          )}
        </button>

        <button
          onClick={() => setIsSlowMode(!isSlowMode)}
          className={`px-6 py-4 rounded-xl flex items-center gap-2 transition-all active:scale-[0.98] ${
            isSlowMode
              ? 'bg-[#FF6B4A] text-white'
              : 'bg-[#F8FAFC] text-[#6B7280] hover:bg-gray-200'
          }`}
        >
          <Gauge className="w-5 h-5" />
          <span>Slow</span>
        </button>
      </div>

      {isSlowMode && (
        <p className="text-xs text-[#6B7280] mt-3 text-center">
          Slow mode enabled for clearer pronunciation
        </p>
      )}
    </div>
  );
}