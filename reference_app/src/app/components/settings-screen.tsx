import { ArrowLeft, Globe2, Gauge, Shield, Download, LogOut, ChevronRight } from 'lucide-react';
import { useState } from 'react';

interface SettingsScreenProps {
  onBack: () => void;
  onLogout: () => void;
}

export function SettingsScreen({ onBack, onLogout }: SettingsScreenProps) {
  const [defaultLanguage, setDefaultLanguage] = useState('English');
  const [playbackSpeed, setPlaybackSpeed] = useState('Normal');
  const [offlineMode, setOfflineMode] = useState(false);

  return (
    <div className="fixed inset-0 bg-[#F8FAFC] z-50 flex flex-col">
      {/* Header */}
      <div className="pt-6 px-6 pb-4 bg-white border-b border-gray-200">
        <button
          onClick={onBack}
          className="p-2 -ml-2 rounded-lg hover:bg-gray-100 transition-colors mb-4"
        >
          <ArrowLeft className="w-6 h-6 text-[#111827]" />
        </button>
        <h2 className="text-xl font-semibold text-[#111827]">Settings</h2>
      </div>

      {/* Settings List */}
      <div className="flex-1 overflow-y-auto">
        {/* Language Settings */}
        <div className="mt-6 px-6">
          <h3 className="text-sm font-medium text-[#6B7280] mb-3 uppercase tracking-wide">
            Translation
          </h3>
          <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
            <button className="w-full px-4 py-4 flex items-center justify-between hover:bg-gray-50 transition-colors border-b border-gray-200">
              <div className="flex items-center gap-3">
                <div className="bg-[#FFF0ED] rounded-lg p-2">
                  <Globe2 className="w-5 h-5 text-[#FF6B4A]" />
                </div>
                <div className="text-left">
                  <p className="text-[#111827] font-medium">Default Language</p>
                  <p className="text-sm text-[#6B7280]">{defaultLanguage}</p>
                </div>
              </div>
              <ChevronRight className="w-5 h-5 text-[#6B7280]" />
            </button>

            <button className="w-full px-4 py-4 flex items-center justify-between hover:bg-gray-50 transition-colors">
              <div className="flex items-center gap-3">
                <div className="bg-[#FFF0ED] rounded-lg p-2">
                  <Gauge className="w-5 h-5 text-[#FF6B4A]" />
                </div>
                <div className="text-left">
                  <p className="text-[#111827] font-medium">Playback Speed</p>
                  <p className="text-sm text-[#6B7280]">{playbackSpeed}</p>
                </div>
              </div>
              <ChevronRight className="w-5 h-5 text-[#6B7280]" />
            </button>
          </div>
        </div>

        {/* Offline Settings */}
        <div className="mt-6 px-6">
          <h3 className="text-sm font-medium text-[#6B7280] mb-3 uppercase tracking-wide">
            Offline
          </h3>
          <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
            <div className="px-4 py-4 flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="bg-[#FFF0ED] rounded-lg p-2">
                  <Download className="w-5 h-5 text-[#FF6B4A]" />
                </div>
                <div className="text-left">
                  <p className="text-[#111827] font-medium">Download Phrases</p>
                  <p className="text-sm text-[#6B7280]">Use offline mode</p>
                </div>
              </div>
              <button
                onClick={() => setOfflineMode(!offlineMode)}
                className={`relative w-12 h-7 rounded-full transition-colors ${
                  offlineMode ? 'bg-[#FF6B4A]' : 'bg-gray-300'
                }`}
              >
                <div
                  className={`absolute top-1 w-5 h-5 bg-white rounded-full transition-transform ${
                    offlineMode ? 'translate-x-6' : 'translate-x-1'
                  }`}
                />
              </button>
            </div>
          </div>
        </div>

        {/* Privacy & Security */}
        <div className="mt-6 px-6">
          <h3 className="text-sm font-medium text-[#6B7280] mb-3 uppercase tracking-wide">
            Privacy & Security
          </h3>
          <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
            <button className="w-full px-4 py-4 flex items-center justify-between hover:bg-gray-50 transition-colors">
              <div className="flex items-center gap-3">
                <div className="bg-[#FFF0ED] rounded-lg p-2">
                  <Shield className="w-5 h-5 text-[#FF6B4A]" />
                </div>
                <div className="text-left">
                  <p className="text-[#111827] font-medium">Privacy Settings</p>
                  <p className="text-sm text-[#6B7280]">Manage your data</p>
                </div>
              </div>
              <ChevronRight className="w-5 h-5 text-[#6B7280]" />
            </button>
          </div>
        </div>

        {/* Account */}
        <div className="mt-6 px-6 pb-6">
          <h3 className="text-sm font-medium text-[#6B7280] mb-3 uppercase tracking-wide">
            Account
          </h3>
          <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
            <button
              onClick={onLogout}
              className="w-full px-4 py-4 flex items-center gap-3 hover:bg-red-50 transition-colors"
            >
              <div className="bg-red-50 rounded-lg p-2">
                <LogOut className="w-5 h-5 text-red-600" />
              </div>
              <p className="text-red-600 font-medium">Log Out</p>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}