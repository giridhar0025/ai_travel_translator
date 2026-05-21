import { useState, useRef, useEffect } from 'react';
import { Send, Mic, Volume2, Edit2, Gauge, ArrowUpDown, Copy } from 'lucide-react';
import { LanguageSelector } from './language-selector';
import { toast } from "sonner";

export interface Message {
  id: string;
  type: 'user' | 'ai' | 'other';
  originalText: string;
  originalLanguage: string;
  translatedText: string;
  translatedLanguage: string;
  pronunciation?: string;
  sender: 'You' | 'Other Person';
}

interface ChatInterfaceProps {
  onVoiceInput: () => void;
  inputText: string;
  setInputText: (text: string) => void;
  onSend: (text: string) => void;
  messages: Message[];
  setMessages: (messages: Message[]) => void;
}

export function ChatInterface({ onVoiceInput, inputText, setInputText, onSend, messages, setMessages }: ChatInterfaceProps) {
  const [sourceLanguage, setSourceLanguage] = useState('English');
  const [targetLanguage, setTargetLanguage] = useState('Hindi');
  const [contextMode, setContextMode] = useState<'formal' | 'casual' | 'tourist'>('casual');
  const [showLanguageReversed, setShowLanguageReversed] = useState(false);
  const [flippedMessages, setFlippedMessages] = useState<Set<string>>(new Set());
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const handleSend = () => {
    if (!inputText.trim()) return;

    // User's message
    const userMessage: Message = {
      id: Date.now().toString(),
      type: 'user',
      originalText: inputText,
      originalLanguage: sourceLanguage,
      translatedText: 'नमस्ते, क्या आप मुझे रेस्तरां का रास्ता बता सकते हैं?',
      translatedLanguage: targetLanguage,
      pronunciation: 'namaste, kya aap mujhe restaurant ka raasta bata sakte hain?',
      sender: 'You'
    };

    // Simulate other person's response
    const otherMessage: Message = {
      id: (Date.now() + 1).toString(),
      type: 'other',
      originalText: 'हां, बिल्कुल! यहाँ से दो ब्लॉक सीधे जाएं।',
      originalLanguage: targetLanguage,
      translatedText: 'Yes, of course! Go straight two blocks from here.',
      translatedLanguage: sourceLanguage,
      pronunciation: 'haan, bilkul! yahaan se do block seedhe jaayein.',
      sender: 'Other Person'
    };

    setMessages([...messages, userMessage, otherMessage]);
    onSend(inputText);
  };

  const toggleFlip = (messageId: string) => {
    const newFlipped = new Set(flippedMessages);
    if (newFlipped.has(messageId)) {
      newFlipped.delete(messageId);
    } else {
      newFlipped.add(messageId);
    }
    setFlippedMessages(newFlipped);
  };

  const handleLanguageChange = (source: string, target: string) => {
    setSourceLanguage(source);
    setTargetLanguage(target);
  };

  const handlePlay = (text: string, lang: string, slow: boolean = false) => {
    if (!('speechSynthesis' in window)) return;
    
    // Cancel any ongoing speech
    window.speechSynthesis.cancel();

    const utterance = new SpeechSynthesisUtterance(text);
    
    // Basic language mapping for speech synthesis
    const langMap: Record<string, string> = {
      'English': 'en-US',
      'Hindi': 'hi-IN',
      'Thai': 'th-TH',
      'Spanish': 'es-ES',
      'French': 'fr-FR',
      'German': 'de-DE',
      'Japanese': 'ja-JP',
      'Korean': 'ko-KR',
      'Chinese': 'zh-CN'
    };

    if (langMap[lang]) {
      utterance.lang = langMap[lang];
    }
    
    if (slow) {
      utterance.rate = 0.5; // Half speed for slow playback
    }

    window.speechSynthesis.speak(utterance);
  };

  const handleCopy = (text: string) => {
    navigator.clipboard.writeText(text);
    toast.success("Copied to clipboard!");
  };

  const handleEdit = (text: string) => {
    setInputText(text);
  };

  return (
    <div className="flex flex-col h-full bg-[#F8FAFC]">
      {/* Header */}
      <div className="bg-white border-b border-gray-200 px-6 py-4 flex-shrink-0">
        <h2 className="text-lg font-semibold text-[#111827]">Translation Chat</h2>
      </div>

      {/* Language Selector */}
      <div className="flex-shrink-0">
        <LanguageSelector
          sourceLanguage={sourceLanguage}
          targetLanguage={targetLanguage}
          onLanguageChange={handleLanguageChange}
        />
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto px-4 py-6 space-y-4">
        {messages.map((message) => {
          const isFlipped = flippedMessages.has(message.id);
          const isUserMessage = message.type === 'user';
          
          return (
            <div
              key={message.id}
              className={`flex ${isUserMessage ? 'justify-end' : 'justify-start'}`}
            >
              {/* Universal Message Card */}
              <div className={`bg-white rounded-2xl ${isUserMessage ? 'rounded-tr-md' : 'rounded-tl-md'} px-4 py-3 max-w-[85%] shadow-sm border ${isUserMessage ? 'border-[#FF6B4A]' : 'border-gray-200'}`}>
                
                {/* Sender Label */}
                <div className="flex items-center justify-between mb-2">
                  <span className={`text-xs font-semibold uppercase tracking-wide ${isUserMessage ? 'text-[#FF6B4A]' : 'text-[#6B7280]'}`}>
                    {message.sender}
                  </span>
                  <button
                    onClick={() => toggleFlip(message.id)}
                    className="p-1 hover:bg-[#FFF0ED] rounded transition-colors"
                    title="Flip languages"
                  >
                    <ArrowUpDown className="w-3.5 h-3.5 text-[#FF6B4A]" />
                  </button>
                </div>

                {!isFlipped ? (
                  <>
                    {/* Original Text Section */}
                    <div className="mb-3">
                      <div className="flex items-center gap-1.5 mb-1.5">
                        <span className="text-lg">{message.originalLanguage === 'Hindi' ? '🇮🇳' : message.originalLanguage === 'Thai' ? '🇹🇭' : '🇬🇧'}</span>
                        <p className="text-xs font-medium text-[#111827] uppercase tracking-wide">
                          {message.originalLanguage}
                        </p>
                      </div>
                      <p className="text-[#111827] text-lg leading-relaxed">{message.originalText}</p>
                      {message.pronunciation && (
                        <p className="text-[#6B7280] text-xs italic mt-1.5">{message.pronunciation}</p>
                      )}
                    </div>

                    {/* Divider */}
                    <div className="border-t border-gray-200 my-3"></div>

                    {/* Translated Text Section */}
                    <div className="mb-3">
                      <div className="flex items-center gap-1.5 mb-1.5">
                        <span className="text-lg">{message.translatedLanguage === 'Hindi' ? '🇮🇳' : message.translatedLanguage === 'Thai' ? '🇹🇭' : '🇬🇧'}</span>
                        <p className="text-xs font-medium text-[#FF6B4A] uppercase tracking-wide">
                          {message.translatedLanguage} (Auto Translation)
                        </p>
                      </div>
                      <p className="text-[#111827] text-base leading-relaxed">{message.translatedText}</p>
                    </div>
                  </>
                ) : (
                  <>
                    {/* Flipped: Translation on top */}
                    <div className="mb-3">
                      <div className="flex items-center gap-1.5 mb-1.5">
                        <span className="text-lg">{message.translatedLanguage === 'Hindi' ? '🇮🇳' : message.translatedLanguage === 'Thai' ? '🇹🇭' : '🇬🇧'}</span>
                        <p className="text-xs font-medium text-[#FF6B4A] uppercase tracking-wide">
                          {message.translatedLanguage} (Auto Translation)
                        </p>
                      </div>
                      <p className="text-[#111827] text-lg leading-relaxed">{message.translatedText}</p>
                    </div>

                    {/* Divider */}
                    <div className="border-t border-gray-200 my-3"></div>

                    {/* Original on bottom */}
                    <div className="mb-3">
                      <div className="flex items-center gap-1.5 mb-1.5">
                        <span className="text-lg">{message.originalLanguage === 'Hindi' ? '🇮🇳' : message.originalLanguage === 'Thai' ? '🇹🇭' : '🇬🇧'}</span>
                        <p className="text-xs font-medium text-[#111827] uppercase tracking-wide">
                          {message.originalLanguage}
                        </p>
                      </div>
                      <p className="text-[#111827] text-base leading-relaxed">{message.originalText}</p>
                      {message.pronunciation && (
                        <p className="text-[#6B7280] text-xs italic mt-1.5">{message.pronunciation}</p>
                      )}
                    </div>
                  </>
                )}

                {/* Action Bar */}
                <div className="flex flex-wrap gap-2 pt-3 border-t border-gray-100">
                  <button 
                    onClick={() => handlePlay(message.translatedText, message.translatedLanguage)}
                    className="flex items-center gap-1.5 bg-[#FFF0ED] text-[#FF6B4A] px-3 py-1.5 rounded-lg text-xs font-medium hover:bg-[#FF6B4A] hover:text-white transition-colors"
                  >
                    <Volume2 className="w-3.5 h-3.5" />
                    <span>Play</span>
                  </button>
                  <button 
                    onClick={() => handlePlay(message.translatedText, message.translatedLanguage, true)}
                    className="flex items-center gap-1.5 bg-[#F8FAFC] text-[#6B7280] px-3 py-1.5 rounded-lg text-xs font-medium hover:bg-gray-200 transition-colors"
                  >
                    <Gauge className="w-3.5 h-3.5" />
                    <span>Slow</span>
                  </button>
                  <button 
                    onClick={() => handleEdit(message.originalText)}
                    className="flex items-center gap-1.5 bg-[#F8FAFC] text-[#6B7280] px-3 py-1.5 rounded-lg text-xs font-medium hover:bg-gray-200 transition-colors"
                  >
                    <Edit2 className="w-3.5 h-3.5" />
                    <span>Edit</span>
                  </button>
                  <button 
                    onClick={() => handleCopy(message.translatedText)}
                    className="flex items-center gap-1.5 bg-[#F8FAFC] text-[#6B7280] px-3 py-1.5 rounded-lg text-xs font-medium hover:bg-gray-200 transition-colors"
                  >
                    <Copy className="w-3.5 h-3.5" />
                    <span>Copy</span>
                  </button>
                </div>
              </div>
            </div>
          );
        })}
        <div ref={messagesEndRef} />
      </div>

      {/* Input Area */}
      <div className="bg-white border-t border-gray-200 px-4 py-4 flex-shrink-0">
        {/* Context Awareness Toggle */}
        <div className="flex gap-2 mb-3 overflow-x-auto pb-2">
          <button
            onClick={() => setContextMode('formal')}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium whitespace-nowrap transition-all ${
              contextMode === 'formal'
                ? 'bg-[#FF6B4A] text-white'
                : 'bg-[#F8FAFC] text-[#6B7280] hover:bg-[#FFF0ED] hover:text-[#FF6B4A]'
            }`}
          >
            👔 Formal tone
          </button>
          <button
            onClick={() => setContextMode('casual')}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium whitespace-nowrap transition-all ${
              contextMode === 'casual'
                ? 'bg-[#FF6B4A] text-white'
                : 'bg-[#F8FAFC] text-[#6B7280] hover:bg-[#FFF0ED] hover:text-[#FF6B4A]'
            }`}
          >
            😊 Casual tone
          </button>
          <button
            onClick={() => setContextMode('tourist')}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium whitespace-nowrap transition-all ${
              contextMode === 'tourist'
                ? 'bg-[#FF6B4A] text-white'
                : 'bg-[#F8FAFC] text-[#6B7280] hover:bg-[#FFF0ED] hover:text-[#FF6B4A]'
            }`}
          >
            🧳 Tourist mode
          </button>
        </div>

        <div className="flex items-end gap-2">
          <button
            onClick={onVoiceInput}
            className="flex-shrink-0 bg-[#FFF0ED] text-[#FF6B4A] p-3 rounded-xl hover:bg-[#FF6B4A] hover:text-white transition-all active:scale-95"
          >
            <Mic className="w-6 h-6" />
          </button>
          
          <div className="flex-1 bg-[#F8FAFC] rounded-xl border border-gray-200 flex items-end">
            <input
              type="text"
              value={inputText}
              onChange={(e) => setInputText(e.target.value)}
              onKeyPress={(e) => e.key === 'Enter' && handleSend()}
              placeholder="Type a message..."
              className="flex-1 bg-transparent px-4 py-3 text-[#111827] placeholder:text-[#6B7280] focus:outline-none"
            />
          </div>
          
          <button
            onClick={handleSend}
            className="flex-shrink-0 bg-[#FF6B4A] text-white p-3 rounded-xl hover:bg-[#E85533] transition-all active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed"
            disabled={!inputText.trim()}
          >
            <Send className="w-6 h-6" />
          </button>
        </div>
      </div>
    </div>
  );
}