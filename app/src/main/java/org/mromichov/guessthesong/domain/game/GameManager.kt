package org.mromichov.guessthesong.domain.game

import org.mromichov.guessthesong.core.exception.TrackCountException
import org.mromichov.guessthesong.core.exception.TrackPreviewException
import org.mromichov.guessthesong.domain.model.GameRound
import org.mromichov.guessthesong.domain.model.Playlist
import org.mromichov.guessthesong.domain.model.Track
import org.mromichov.guessthesong.domain.model.TrackPreview
import org.mromichov.guessthesong.domain.repository.TrackPreviewRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameManager @Inject constructor(
    private val trackPreviewRepository: TrackPreviewRepository,
) {
    companion object {
        private const val WRONG_OPTIONS_NUMBER: Int = 3
        private const val MAX_SNIPPET_LENGTH_MS: Long = 30_000L
    }

    var currentRoundIndex: Int = 0
        private set

    var score: Int = 0
        private set

    lateinit var config: GameConfig
        private set

    private val rounds = mutableListOf<GameRound>()
    val currentRoundOptions: MutableList<Track>
        get() {
            val options = mutableListOf(getCurrentRound().track)
            options.addAll(getCurrentRound().wrongOptions)
            options.shuffle()
            return options
        }

    fun checkAnswer(userAnswer: Track): Boolean {
        val rightAnswer = rounds[currentRoundIndex].track
        val isGuessed = rightAnswer == userAnswer
        if (isGuessed) score++
        return isGuessed
    }

    fun getCurrentRound(): GameRound {
        return rounds[currentRoundIndex]
    }

    fun getCurrentTrackPreview(): TrackPreview {
        return rounds[currentRoundIndex].trackPreview
    }

    fun nextRound() {
        currentRoundIndex++
    }


    suspend fun prepareRounds(playlist: Playlist, config: GameConfig) {
        validateConfig(config)
        this.config = config
        validatePlaylist(playlist)
        val alreadySelected = mutableSetOf<Track>()

        while (rounds.size != this.config.roundsNumber) {
            val track = playlist.tracks.random()
            if (alreadySelected.contains(track)) continue

            alreadySelected.add(track)
            val round = prepareOneRound(track, playlist.tracks)
            rounds.add(round)
        }
    }

    private suspend fun prepareOneRound(track: Track, allTracksInPlaylist: List<Track>): GameRound {
        val wrongOptions = mutableListOf<Track>()
        while (wrongOptions.size != WRONG_OPTIONS_NUMBER) {
            val randomTrack = allTracksInPlaylist.random()
            if (randomTrack != track && randomTrack !in wrongOptions) {
                wrongOptions.add(randomTrack)
            }
        }
        val trackPreview: TrackPreview = trackPreviewRepository
            .getPreview(track.artist, track.title)
            .getOrElse {
                throw TrackPreviewException()
            }

        return GameRound(track, trackPreview, wrongOptions)
    }

    fun reset() {
        currentRoundIndex = 0
        score = 0
        rounds.removeAll { true }
    }
    private fun validateConfig(config: GameConfig) {
        require(config.roundsNumber > 0) {
            "Number of rounds must be positive"
        }
        require(config.snippetLength in 1L until MAX_SNIPPET_LENGTH_MS) {
            "Snippet length must be between 1 and ${MAX_SNIPPET_LENGTH_MS - 1} ms"
        }
    }

    private fun validatePlaylist(playlist: Playlist) {
        if (playlist.tracks.size < config.roundsNumber) {
            throw TrackCountException()
        }
    }
}
