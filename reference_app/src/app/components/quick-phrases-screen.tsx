import { ArrowLeft, UtensilsCrossed, Navigation, Car, ShoppingBag, AlertCircle, ChevronRight, Star, Clock } from 'lucide-react';
import { useState } from 'react';

interface QuickPhrasesScreenProps {
  onBack: () => void;
  onPhraseSelect: (phrase: string) => void;
}

export function QuickPhrasesScreen({ onBack, onPhraseSelect }: QuickPhrasesScreenProps) {
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [favoritePhrases, setFavoritePhrases] = useState<string[]>([
    'Table for two, please',
    'Where is the nearest subway?',
    'How much does this cost?'
  ]);
  const [recentPhrases, setRecentPhrases] = useState<string[]>([
    'The bill, please',
    'Can you show me on the map?',
    'One ticket, please'
  ]);

  const categories = [
    {
      id: 'restaurants',
      name: 'Restaurants',
      icon: <UtensilsCrossed className="w-6 h-6" />,
      color: 'bg-orange-100 text-orange-600',
      phrases: [
        'Table for two, please',
        'Can I see the menu?',
        'I would like to order',
        'The bill, please',
        'Do you have vegetarian options?',
        'Is this spicy?'
      ]
    },
    {
      id: 'directions',
      name: 'Directions',
      icon: <Navigation className="w-6 h-6" />,
      color: 'bg-blue-100 text-blue-600',
      phrases: [
        'Where is the nearest subway?',
        'How do I get to...?',
        'Can you show me on the map?',
        'Is it walking distance?',
        'Which way is north?',
        'How far is it?'
      ]
    },
    {
      id: 'transport',
      name: 'Transport',
      icon: <Car className="w-6 h-6" />,
      color: 'bg-green-100 text-green-600',
      phrases: [
        'How much to the airport?',
        'One ticket, please',
        'Does this bus go to...?',
        'What time is the next train?',
        'Can you call me a taxi?',
        'Stop here, please'
      ]
    },
    {
      id: 'shopping',
      name: 'Shopping',
      icon: <ShoppingBag className="w-6 h-6" />,
      color: 'bg-purple-100 text-purple-600',
      phrases: [
        'How much does this cost?',
        'Can I try this on?',
        'Do you accept credit cards?',
        'I\'m just looking',
        'Do you have a smaller size?',
        'Can I get a discount?'
      ]
    },
    {
      id: 'emergencies',
      name: 'Emergencies',
      icon: <AlertCircle className="w-6 h-6" />,
      color: 'bg-red-100 text-red-600',
      phrases: [
        'I need help',
        'Where is the hospital?',
        'Call the police',
        'I lost my passport',
        'I need a doctor',
        'This is an emergency'
      ]
    }
  ];

  const toggleFavorite = (phrase: string) => {
    if (favoritePhrases.includes(phrase)) {
      setFavoritePhrases(favoritePhrases.filter(p => p !== phrase));
    } else {
      setFavoritePhrases([...favoritePhrases, phrase]);
    }
  };

  const handlePhraseClick = (phrase: string) => {
    // Add to recent if not already there
    if (!recentPhrases.includes(phrase)) {
      setRecentPhrases([phrase, ...recentPhrases.slice(0, 4)]); // Keep last 5
    }
    onPhraseSelect(phrase);
    setSelectedCategory(null);
    onBack();
  };

  if (selectedCategory) {
    const category = categories.find(c => c.id === selectedCategory);
    if (!category) return null;

    return (
      <div className="fixed inset-0 bg-[#F8FAFC] z-50 flex flex-col">
        {/* Header */}
        <div className="pt-6 px-6 pb-4 bg-white border-b border-gray-200">
          <button
            onClick={() => setSelectedCategory(null)}
            className="p-2 -ml-2 rounded-lg hover:bg-gray-100 transition-colors mb-4"
          >
            <ArrowLeft className="w-6 h-6 text-[#111827]" />
          </button>
          <h2 className="text-xl font-semibold text-[#111827]">{category.name}</h2>
        </div>

        {/* Phrases */}
        <div className="flex-1 overflow-y-auto px-6 py-6">
          <div className="space-y-3">
            {category.phrases.map((phrase, index) => {
              const isFavorite = favoritePhrases.includes(phrase);
              return (
                <div
                  key={index}
                  className="w-full bg-white border border-gray-200 rounded-xl overflow-hidden hover:border-[#FF6B4A] hover:bg-[#FFF0ED] transition-all group"
                >
                  <div className="flex items-center">
                    <button
                      onClick={() => handlePhraseClick(phrase)}
                      className="flex-1 px-5 py-4 text-left"
                    >
                      <p className="text-[#111827] group-hover:text-[#FF6B4A]">{phrase}</p>
                    </button>
                    <button
                      onClick={() => toggleFavorite(phrase)}
                      className="px-4 py-4 hover:bg-[#FFF0ED] transition-colors"
                    >
                      <Star
                        className={`w-5 h-5 transition-colors ${
                          isFavorite
                            ? 'text-[#F59E0B] fill-[#F59E0B]'
                            : 'text-[#6B7280]'
                        }`}
                      />
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    );
  }

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
        <h2 className="text-xl font-semibold text-[#111827]">Quick Phrases</h2>
        <p className="text-sm text-[#6B7280] mt-1">Common phrases for travelers</p>
      </div>

      {/* Favorites & Recent */}
      <div className="flex-1 overflow-y-auto">
        {/* Favorites Section */}
        {favoritePhrases.length > 0 && (
          <div className="px-6 py-6 border-b border-gray-200">
            <div className="flex items-center gap-2 mb-4">
              <Star className="w-5 h-5 text-[#F59E0B] fill-[#F59E0B]" />
              <h3 className="text-sm font-semibold text-[#111827] uppercase tracking-wide">Favorites</h3>
            </div>
            <div className="space-y-2">
              {favoritePhrases.map((phrase, index) => (
                <div
                  key={index}
                  className="w-full bg-white border border-gray-200 rounded-xl overflow-hidden hover:border-[#FF6B4A] hover:bg-[#FFF0ED] transition-all group"
                >
                  <div className="flex items-center">
                    <button
                      onClick={() => handlePhraseClick(phrase)}
                      className="flex-1 px-5 py-3 text-left"
                    >
                      <p className="text-[#111827] group-hover:text-[#FF6B4A] text-sm">{phrase}</p>
                    </button>
                    <button
                      onClick={() => toggleFavorite(phrase)}
                      className="px-4 py-3 hover:bg-[#FFF0ED] transition-colors"
                    >
                      <Star className="w-4 h-4 text-[#F59E0B] fill-[#F59E0B]" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Recent Section */}
        {recentPhrases.length > 0 && (
          <div className="px-6 py-6 border-b border-gray-200">
            <div className="flex items-center gap-2 mb-4">
              <Clock className="w-5 h-5 text-[#6B7280]" />
              <h3 className="text-sm font-semibold text-[#111827] uppercase tracking-wide">Recent</h3>
            </div>
            <div className="space-y-2">
              {recentPhrases.map((phrase, index) => (
                <button
                  key={index}
                  onClick={() => handlePhraseClick(phrase)}
                  className="w-full bg-white border border-gray-200 rounded-xl px-5 py-3 text-left hover:border-[#FF6B4A] hover:bg-[#FFF0ED] transition-all group"
                >
                  <p className="text-[#111827] group-hover:text-[#FF6B4A] text-sm">{phrase}</p>
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Categories */}
        <div className="px-6 py-6">
          <h3 className="text-sm font-semibold text-[#6B7280] uppercase tracking-wide mb-4">All Categories</h3>
          <div className="space-y-3">
            {categories.map((category) => (
              <button
                key={category.id}
                onClick={() => setSelectedCategory(category.id)}
                className="w-full bg-white border border-gray-200 rounded-xl px-5 py-4 flex items-center justify-between hover:border-[#FF6B4A] hover:shadow-md transition-all active:scale-[0.98]"
              >
                <div className="flex items-center gap-4">
                  <div className={`${category.color} rounded-xl p-3`}>
                    {category.icon}
                  </div>
                  <span className="text-[#111827] font-medium">{category.name}</span>
                </div>
                <ChevronRight className="w-5 h-5 text-[#6B7280]" />
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
