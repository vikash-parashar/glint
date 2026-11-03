package app.glint.widgets.store

import java.time.LocalDate

/** Public-domain quotes, one per day, same for everyone on a given date. */
object Quotes {
    data class Quote(val text: String, val author: String)

    private val list = listOf(
        Quote("Well begun is half done.", "Aristotle"),
        Quote("We suffer more often in imagination than in reality.", "Seneca"),
        Quote("Luck is what happens when preparation meets opportunity.", "Seneca"),
        Quote("The happiness of your life depends upon the quality of your thoughts.", "Marcus Aurelius"),
        Quote("Waste no more time arguing what a good man should be. Be one.", "Marcus Aurelius"),
        Quote("It does not matter how slowly you go as long as you do not stop.", "Confucius"),
        Quote("The journey of a thousand miles begins with one step.", "Lao Tzu"),
        Quote("Knowing yourself is the beginning of all wisdom.", "Aristotle"),
        Quote("Simplicity is the ultimate sophistication.", "Leonardo da Vinci"),
        Quote("Energy and persistence conquer all things.", "Benjamin Franklin"),
        Quote("Well done is better than well said.", "Benjamin Franklin"),
        Quote("Lost time is never found again.", "Benjamin Franklin"),
        Quote("Nothing will come of nothing.", "William Shakespeare"),
        Quote("What we think, we become.", "Buddha"),
        Quote("Do what you can, with what you have, where you are.", "Theodore Roosevelt"),
        Quote("Believe you can and you're halfway there.", "Theodore Roosevelt"),
        Quote("Arise, awake, and stop not till the goal is reached.", "Swami Vivekananda"),
        Quote("Take up one idea. Make that one idea your life.", "Swami Vivekananda"),
        Quote("Let us sacrifice our today so that our children can have a better tomorrow.", "A. P. J. Abdul Kalam"),
        Quote("Where the mind is without fear and the head is held high.", "Rabindranath Tagore"),
        Quote("Faith is the bird that feels the light when the dawn is still dark.", "Rabindranath Tagore"),
        Quote("An ounce of practice is worth more than tons of preaching.", "Mahatma Gandhi"),
        Quote("The future depends on what you do today.", "Mahatma Gandhi"),
        Quote("Live as if you were to die tomorrow. Learn as if you were to live forever.", "Mahatma Gandhi"),
        Quote("He who has a why to live can bear almost any how.", "Friedrich Nietzsche"),
        Quote("The only true wisdom is in knowing you know nothing.", "Socrates"),
        Quote("Patience is bitter, but its fruit is sweet.", "Jean-Jacques Rousseau"),
        Quote("Quality is not an act, it is a habit.", "Aristotle"),
        Quote("I think, therefore I am.", "René Descartes"),
        Quote("Genius is one percent inspiration and ninety-nine percent perspiration.", "Thomas Edison"),
        Quote("Be not afraid of going slowly; be afraid only of standing still.", "Chinese proverb"),
        Quote("Fall seven times, stand up eight.", "Japanese proverb"),
    )

    fun forDate(date: LocalDate = LocalDate.now()): Quote = list[(date.toEpochDay() % list.size).toInt()]
}
