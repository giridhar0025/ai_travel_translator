import { MessageCircle, BookText, History, Settings } from 'lucide-react';

interface NavigationBarProps {
  activeTab: string;
  onTabChange: (tab: string) => void;
}

export function NavigationBar({ activeTab, onTabChange }: NavigationBarProps) {
  const tabs = [
    { id: 'chat', label: 'Chat', icon: MessageCircle },
    { id: 'phrases', label: 'Phrases', icon: BookText },
    { id: 'history', label: 'History', icon: History },
    { id: 'settings', label: 'Settings', icon: Settings }
  ];

  return (
    <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 px-2 pb-safe">
      <div className="flex items-center justify-around h-16">
        {tabs.map(({ id, label, icon: Icon }) => (
          <button
            key={id}
            onClick={() => onTabChange(id)}
            className={`flex-1 flex flex-col items-center justify-center gap-1 py-2 px-3 rounded-lg transition-colors ${
              activeTab === id
                ? 'text-[#FF6B4A]'
                : 'text-[#6B7280] hover:text-[#111827]'
            }`}
          >
            <Icon className="w-6 h-6" strokeWidth={activeTab === id ? 2.5 : 2} />
            <span className="text-xs font-medium">{label}</span>
          </button>
        ))}
      </div>
    </div>
  );
}
