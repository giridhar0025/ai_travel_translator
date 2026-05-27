package com.divami.traveltranslator.feature.chat

import androidx.lifecycle.ViewModel
import com.divami.traveltranslator.designsystem.components.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.absoluteValue

data class ChatUiState(
    // #16: default is now empty list — empty-state UI is shown in ChatScreen when messages.isEmpty()
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val sourceLanguage: AppLanguage = AppLanguage.English,
    val targetLanguage: AppLanguage = AppLanguage.Hindi,
    val contextMode: ContextMode = ContextMode.Casual,
    val flippedIds: Set<String> = emptySet(),
    // #17: nullable error string; set to non-null to surface an inline error chip in ChatScreen
    val error: String? = null,
)

class ChatViewModel : ViewModel() {
    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    fun onInputChange(text: String) {
        _state.update { it.copy(input = text) }
    }

    fun onContextChange(mode: ContextMode) {
        _state.update { it.copy(contextMode = mode) }
    }

    fun onSourceLanguageChange(lang: AppLanguage) {
        _state.update { it.copy(sourceLanguage = lang) }
    }

    fun onTargetLanguageChange(lang: AppLanguage) {
        _state.update { it.copy(targetLanguage = lang) }
    }

    fun swapLanguages() {
        _state.update { it.copy(sourceLanguage = it.targetLanguage, targetLanguage = it.sourceLanguage) }
    }

    fun toggleFlip(messageId: String) {
        _state.update {
            val next = it.flippedIds.toMutableSet().apply {
                if (!add(messageId)) remove(messageId)
            }
            it.copy(flippedIds = next)
        }
    }

    // #17: dismiss the inline error chip
    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    fun onSend() {
        val input = _state.value.input.trim()
        if (input.isEmpty()) return
        val now = System.currentTimeMillis()
        val src = _state.value.sourceLanguage
        val tgt = _state.value.targetLanguage

        // #18: hash-based index so each distinct input picks a different pool entry
        val userIdx = input.hashCode().absoluteValue % MOCK_POOL_SIZE
        val replyIdx = (input.hashCode().absoluteValue + 3) % MOCK_POOL_SIZE // offset so reply differs from user msg

        val outgoing = ChatMessage(
            id = "u-$now",
            sender = Sender.You,
            originalText = input,
            originalLanguage = src,
            translatedText = mockTranslation(userIdx, tgt),
            translatedLanguage = tgt,
            pronunciation = mockPronunciation(userIdx, tgt),
        )
        val reply = ChatMessage(
            id = "o-${now + 1}",
            sender = Sender.OtherPerson,
            originalText = mockReply(replyIdx, tgt),
            originalLanguage = tgt,
            translatedText = mockReplyTranslation(replyIdx),
            translatedLanguage = src,
            pronunciation = if (tgt.code != "en") mockPronunciation(replyIdx, tgt) else null,
        )

        // TODO(audit-#17): emit error from ChatViewModel.onSend when translation fails (Phase 4)

        _state.update {
            it.copy(input = "", messages = it.messages + outgoing + reply)
        }
    }

    // ---------------------------------------------------------------------------
    // #18 — varied mock reply pools (5 entries per language)
    // ---------------------------------------------------------------------------

    private companion object {
        const val MOCK_POOL_SIZE = 5

        // English originals for the "user says" translations
        private val USER_TRANSLATIONS_HI = listOf(
            "नमस्ते, क्या आप मुझे रेस्तरां का रास्ता बता सकते हैं?",
            "मुझे माफ़ करें, क्या आप इसे दोबारा कह सकते हैं?",
            "यह बहुत अच्छा लगता है, चलिए करते हैं!",
            "इसकी कीमत कितनी है?",
            "बहुत बहुत धन्यवाद, मैं वास्तव में इसकी सराहना करता हूँ!",
        )
        private val USER_TRANSLATIONS_TE = listOf(
            "నమస్కారం, మీరు నాకు రెస్టారెంట్ దారి చెప్పగలరా?",
            "క్షమించండి, మీరు దాన్ని మళ్ళీ చెప్పగలరా?",
            "అది చాలా బాగుంది, చేద్దాం!",
            "ఇది ఎంత ధర?",
            "చాలా చాలా ధన్యవాదాలు, నేను నిజంగా అభినందిస్తున్నాను!",
        )
        private val USER_TRANSLATIONS_TH = listOf(
            "สวัสดี คุณช่วยบอกทางไปร้านอาหารได้ไหม?",
            "ขอโทษ คุณพูดซ้ำอีกครั้งได้ไหม?",
            "ฟังดูดีมาก มาทำกันเลย!",
            "ราคาเท่าไหร่?",
            "ขอบคุณมากๆ ฉันขอบคุณจริงๆ!",
        )

        // OtherPerson replies in the target language
        private val REPLY_HI = listOf(
            "हां, बिल्कुल! यहाँ से दो ब्लॉक सीधे जाएं।",
            "माफ़ करें, मैं समझा नहीं। क्या आप दोबारा कह सकते हैं?",
            "यह बेहतरीन लगता है। आइए करते हैं!",
            "इसकी कीमत दो सौ रुपये है।",
            "आपका बहुत बहुत शुक्रिया!",
        )
        private val REPLY_TE = listOf(
            "అవును, తప్పకుండా! ఇక్కడ నుండి రెండు బ్లాకులు సూటిగా వెళ్లండి.",
            "క్షమించండి, నాకు అర్థం కాలేదు. మళ్ళీ చెప్పగలరా?",
            "అది చాలా మంచిగా అనిపిస్తోంది. చేద్దాం!",
            "ఇది రెండు వందల రూపాయలు.",
            "చాలా చాలా ధన్యవాదాలు!",
        )
        private val REPLY_TH = listOf(
            "ใช่ แน่นอน! เดินตรงไปสองช่วงตึกจากที่นี่",
            "ขอโทษ ฉันไม่เข้าใจ คุณพูดซ้ำได้ไหม?",
            "ฟังดูดีมาก มาทำกันเลย!",
            "ราคาสองร้อยบาทครับ",
            "ขอบคุณมากๆ เลยนะ!",
        )
        private val REPLY_EN = listOf(
            "Yes, of course! Go straight two blocks from here.",
            "Sorry, I don't understand. Could you repeat that?",
            "That sounds great. Let's do it!",
            "How much does it cost?",
            "Thank you so much, I really appreciate it!",
        )

        // English back-translations of the OtherPerson replies (for the translated side)
        private val REPLY_TRANSLATIONS_EN = listOf(
            "Yes, of course! Go straight two blocks from here.",
            "Sorry, I don't understand. Could you repeat that?",
            "That sounds great. Let's do it!",
            "It costs two hundred rupees.",
            "Thank you so much, I really appreciate it!",
        )

        // Romanised pronunciations per pool index
        private val PRONUN_HI = listOf(
            "haan, bilkul! yahaan se do block seedhe jaaein.",
            "maaf karo, main samjha nahin. kya aap dobara keh sakte hain?",
            "yeh behtareen lagta hai. aao karte hain!",
            "iski keemat do sau rupaye hai.",
            "aapka bahut bahut shukriya!",
        )
        private val PRONUN_TE = listOf(
            "avunu, tappakunda! ikkadi nundi rendu blocks sootigaa vellanDi.",
            "kshamiNchanDi, naaku artham kaaledhu. mallii cheppagalaraa?",
            "adi chaala manchigaa anipistondhi. cheddaam!",
            "idi rendu vandala rupaayalu.",
            "chaala chaala dhanyavaadaalu!",
        )
        private val PRONUN_TH = listOf(
            "chai, naenon! dern trong pai sawng chuang tuek jaak tee nee.",
            "kho toad, chan mai khao jai. khun pood saem dai mai?",
            "fang du dee mak. ma tam kan loei!",
            "ra-kha sawng roi baht.",
            "khob khun mak mak loei na!",
        )
    }

    private fun mockTranslation(idx: Int, target: AppLanguage): String = when (target.code) {
        "hi" -> USER_TRANSLATIONS_HI[idx]
        "te" -> USER_TRANSLATIONS_TE[idx]
        "th" -> USER_TRANSLATIONS_TH[idx]
        else -> REPLY_EN[idx] // English target → echo with pool variety
    }

    private fun mockReply(idx: Int, target: AppLanguage): String = when (target.code) {
        "hi" -> REPLY_HI[idx]
        "te" -> REPLY_TE[idx]
        "th" -> REPLY_TH[idx]
        else -> REPLY_EN[idx]
    }

    private fun mockReplyTranslation(idx: Int): String = REPLY_TRANSLATIONS_EN[idx]

    private fun mockPronunciation(idx: Int, target: AppLanguage): String = when (target.code) {
        "hi" -> PRONUN_HI[idx]
        "te" -> PRONUN_TE[idx]
        "th" -> PRONUN_TH[idx]
        else -> ""
    }
}
