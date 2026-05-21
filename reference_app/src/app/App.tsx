import { useState } from 'react';
import { SplashScreen } from './components/splash-screen';
import { LoginScreen } from './components/login-screen';
import { EmailLoginScreen } from './components/email-login-screen';
import { SignUpScreen } from './components/signup-screen';
import { ForgotPasswordScreen } from './components/forgot-password-screen';
import { OnboardingFlow } from './components/onboarding-flow';
import { ChatInterface, Message } from './components/chat-interface';
import { VoiceInputScreen } from './components/voice-input-screen';
import { QuickPhrasesScreen } from './components/quick-phrases-screen';
import { ConversationHistoryScreen } from './components/conversation-history-screen';
import { SettingsScreen } from './components/settings-screen';
import { NavigationBar } from './components/navigation-bar';
import { WelcomeOverview } from './components/welcome-overview';
import { Toaster } from './components/ui/sonner';

type AppScreen = 
  | 'welcome'
  | 'splash'
  | 'login'
  | 'email-login'
  | 'signup'
  | 'forgot-password'
  | 'onboarding'
  | 'chat'
  | 'voice-input'
  | 'quick-phrases'
  | 'history'
  | 'settings';

export default function App() {
  const [currentScreen, setCurrentScreen] = useState<AppScreen>('welcome');
  const [activeTab, setActiveTab] = useState('chat');
  const [showVoiceInput, setShowVoiceInput] = useState(false);
  const [showQuickPhrases, setShowQuickPhrases] = useState(false);
  const [showHistory, setShowHistory] = useState(false);
  const [showSettings, setShowSettings] = useState(false);
  const [chatInput, setChatInput] = useState('');
  const [chatMessages, setChatMessages] = useState<Message[]>([
    {
      id: '1',
      type: 'ai',
      originalText: 'Hi! I can help you translate anything. Just type or speak.',
      originalLanguage: 'English',
      translatedText: 'नमस्ते! मैं आपकी किसी भी चीज़ का अनुवाद मदद कर सकता हूँ। सिर्फ टाइप या बोलें।',
      translatedLanguage: 'Hindi',
      sender: 'AI'
    }
  ]);

  // Handle welcome screen
  const handleStartExploring = () => {
    setCurrentScreen('splash');
  };

  // Handle authentication flow
  const handleSplashComplete = () => {
    setCurrentScreen('login');
  };

  const handleSkipLogin = () => {
    setCurrentScreen('onboarding');
  };

  const handleGoogleLogin = () => {
    setCurrentScreen('onboarding');
  };

  const handleAppleLogin = () => {
    setCurrentScreen('onboarding');
  };

  const handleEmailLoginScreen = () => {
    setCurrentScreen('email-login');
  };

  const handleLogin = () => {
    setCurrentScreen('onboarding');
  };

  const handleSignUpScreen = () => {
    setCurrentScreen('signup');
  };

  const handleSignUp = () => {
    setCurrentScreen('onboarding');
  };

  const handleForgotPassword = () => {
    setCurrentScreen('forgot-password');
  };

  const handleBackToLogin = () => {
    setCurrentScreen('login');
  };

  const handleOnboardingComplete = () => {
    setCurrentScreen('chat');
  };

  // Handle navigation
  const handleTabChange = (tab: string) => {
    setActiveTab(tab);
    setShowVoiceInput(false);
    setShowQuickPhrases(false);
    setShowHistory(false);
    setShowSettings(false);
    
    if (tab === 'chat') {
      setCurrentScreen('chat');
    } else if (tab === 'phrases') {
      setShowQuickPhrases(true);
    } else if (tab === 'history') {
      setShowHistory(true);
    } else if (tab === 'settings') {
      setShowSettings(true);
    }
  };

  const handleVoiceInput = () => {
    setShowVoiceInput(true);
  };

  const handleVoiceInputBack = () => {
    setShowVoiceInput(false);
  };

  const handleTranslate = (text: string) => {
    setChatInput(text);
    setShowVoiceInput(false);
  };

  const handlePhraseSelect = (phrase: string) => {
    setChatInput(phrase);
    setShowQuickPhrases(false);
  };

  const handleChatSend = () => {
    setChatInput('');
  };

  const handleConversationOpen = (id: string) => {
    console.log('Opening conversation:', id);
    setShowHistory(false);
    setCurrentScreen('chat');
    setActiveTab('chat');
  };

  const handleLogout = () => {
    setCurrentScreen('login');
    setShowSettings(false);
    setActiveTab('chat');
  };

  // Render screens based on state
  if (currentScreen === 'welcome') {
    return <WelcomeOverview onStart={handleStartExploring} />;
  }

  if (currentScreen === 'splash') {
    return <SplashScreen onComplete={handleSplashComplete} />;
  }

  if (currentScreen === 'login') {
    return (
      <LoginScreen
        onGoogleLogin={handleGoogleLogin}
        onAppleLogin={handleAppleLogin}
        onEmailLogin={handleEmailLoginScreen}
        onSkip={handleSkipLogin}
      />
    );
  }

  if (currentScreen === 'email-login') {
    return (
      <EmailLoginScreen
        onBack={handleBackToLogin}
        onLogin={handleLogin}
        onForgotPassword={handleForgotPassword}
        onSignUp={handleSignUpScreen}
      />
    );
  }

  if (currentScreen === 'signup') {
    return (
      <SignUpScreen
        onBack={handleBackToLogin}
        onSignUp={handleSignUp}
      />
    );
  }

  if (currentScreen === 'forgot-password') {
    return (
      <ForgotPasswordScreen
        onBack={handleBackToLogin}
      />
    );
  }

  if (currentScreen === 'onboarding') {
    return <OnboardingFlow onComplete={handleOnboardingComplete} />;
  }

  // Main app (after authentication)
  return (
    <div className="relative max-w-md mx-auto bg-white shadow-2xl h-screen overflow-hidden">
      {/* Main Chat Interface */}
      {currentScreen === 'chat' && (
        <div className="h-full pb-16">
          <ChatInterface 
            onVoiceInput={handleVoiceInput}
            inputText={chatInput}
            setInputText={setChatInput}
            messages={chatMessages}
            setMessages={setChatMessages}
            onSend={handleChatSend}
          />
        </div>
      )}
      
      {/* Overlays */}
      {showVoiceInput && (
        <VoiceInputScreen
          onBack={handleVoiceInputBack}
          onTranslate={handleTranslate}
        />
      )}

      {showQuickPhrases && (
        <QuickPhrasesScreen
          onBack={() => setShowQuickPhrases(false)}
          onPhraseSelect={handlePhraseSelect}
        />
      )}

      {showHistory && (
        <ConversationHistoryScreen
          onBack={() => setShowHistory(false)}
          onConversationOpen={handleConversationOpen}
        />
      )}

      {showSettings && (
        <SettingsScreen
          onBack={() => setShowSettings(false)}
          onLogout={handleLogout}
        />
      )}

      {/* Bottom Navigation */}
      {currentScreen === 'chat' && (
        <NavigationBar
          activeTab={activeTab}
          onTabChange={handleTabChange}
        />
      )}
      <Toaster />
    </div>
  );
}