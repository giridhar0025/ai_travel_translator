import { ArrowLeft, Search, MessageCircle, ChevronRight } from 'lucide-react';
import { useState } from 'react';

interface ConversationHistoryScreenProps {
  onBack: () => void;
  onConversationOpen: (id: string) => void;
}

export function ConversationHistoryScreen({ onBack, onConversationOpen }: ConversationHistoryScreenProps) {
  const [searchQuery, setSearchQuery] = useState('');

  const conversations = [
    {
      id: '1',
      title: 'Restaurant Conversation',
      preview: 'Table for two, please...',
      date: 'Today',
      time: '2:30 PM',
      messageCount: 8
    },
    {
      id: '2',
      title: 'Directions to Museum',
      preview: 'How do I get to the museum?',
      date: 'Today',
      time: '10:15 AM',
      messageCount: 5
    },
    {
      id: '3',
      title: 'Hotel Check-in',
      preview: 'I have a reservation...',
      date: 'Yesterday',
      time: '6:45 PM',
      messageCount: 12
    },
    {
      id: '4',
      title: 'Shopping at Market',
      preview: 'How much does this cost?',
      date: 'Yesterday',
      time: '3:20 PM',
      messageCount: 15
    },
    {
      id: '5',
      title: 'Taxi to Airport',
      preview: 'How much to the airport?',
      date: 'Feb 16',
      time: '7:00 AM',
      messageCount: 4
    }
  ];

  const groupedConversations = conversations.reduce((acc, conv) => {
    if (!acc[conv.date]) {
      acc[conv.date] = [];
    }
    acc[conv.date].push(conv);
    return acc;
  }, {} as Record<string, typeof conversations>);

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
        <h2 className="text-xl font-semibold text-[#111827] mb-4">History</h2>
        
        {/* Search Bar */}
        <div className="relative">
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-[#6B7280]" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search conversations..."
            className="w-full bg-[#F8FAFC] border border-gray-200 rounded-xl pl-12 pr-4 py-3 text-[#111827] placeholder:text-[#6B7280] focus:outline-none focus:ring-2 focus:ring-[#FF6B4A] focus:border-transparent"
          />
        </div>
      </div>

      {/* Conversations List */}
      <div className="flex-1 overflow-y-auto">
        {Object.entries(groupedConversations).map(([date, convs]) => (
          <div key={date} className="mb-6">
            {/* Date Header */}
            <div className="px-6 py-2 bg-[#F8FAFC] sticky top-0">
              <p className="text-sm font-medium text-[#6B7280]">{date}</p>
            </div>
            
            {/* Conversations */}
            <div className="px-6 py-2 space-y-2">
              {convs.map((conv) => (
                <button
                  key={conv.id}
                  onClick={() => onConversationOpen(conv.id)}
                  className="w-full bg-white border border-gray-200 rounded-xl p-4 flex items-start gap-3 hover:border-[#FF6B4A] hover:shadow-md transition-all active:scale-[0.98] group"
                >
                  <div className="flex-shrink-0 bg-[#FFF0ED] rounded-xl p-2.5 group-hover:bg-[#FF6B4A] transition-colors">
                    <MessageCircle className="w-5 h-5 text-[#FF6B4A] group-hover:text-white" />
                  </div>
                  
                  <div className="flex-1 text-left min-w-0">
                    <div className="flex items-start justify-between gap-2 mb-1">
                      <h3 className="font-medium text-[#111827] truncate">
                        {conv.title}
                      </h3>
                      <span className="text-xs text-[#6B7280] flex-shrink-0">
                        {conv.time}
                      </span>
                    </div>
                    <p className="text-sm text-[#6B7280] truncate">
                      {conv.preview}
                    </p>
                    <p className="text-xs text-[#6B7280] mt-1">
                      {conv.messageCount} messages
                    </p>
                  </div>
                  
                  <ChevronRight className="w-5 h-5 text-[#6B7280] flex-shrink-0 self-center" />
                </button>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}