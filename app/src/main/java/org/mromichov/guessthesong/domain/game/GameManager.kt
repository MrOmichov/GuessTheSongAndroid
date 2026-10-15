package org.mromichov.guessthesong.domain.game

import org.mromichov.guessthesong.core.AppConsts.Companion.MAX_SNIPPET_LENGTH_IN_SECONDS
import org.mromichov.guessthesong.core.AppConsts.Companion.MIN_SNIPPET_LENGTH_IN_SECONDS
import org.mromichov.guessthesong.core.AppConsts.Companion.WRONG_OPTIONS_NUMBER
import org.mromichov.guessthesong.core.exception.TrackCountException
import org.mromichov.guessthesong.core.exception.TrackPreviewCountException
import org.mromichov.guessthesong.core.exception.TrackPreviewNotFoundException
import org.mromichov.guessthesong.domain.model.GameRound
import org.mromichov.guessthesong.domain.model.Track
import org.mromichov.guessthesong.domain.model.TrackPreview
import org.mromichov.guessthesong.domain.model.playlist.PlaylistContent
import org.mromichov.guessthesong.domain.repository.TrackPreviewRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameManager @Inject constructor(
    private val trackPreviewRepository: TrackPreviewRepository,
) {
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

    suspend fun prepareRounds(playlist: PlaylistContent, config: GameConfig) {
        validateConfig(config)
        this.config = config
        val uniqueShuffledTracks = validatePlaylistAndGetUniqueTracks(playlist).shuffled()
        val alreadySelected = mutableSetOf<Track>()

        for (answerTrack in uniqueShuffledTracks) {
            if (rounds.size == config.roundsNumber) break
            if (alreadySelected.contains(answerTrack)) continue

            alreadySelected.add(answerTrack)
            prepareOneRound(answerTrack, uniqueShuffledTracks)
                .onSuccess { rounds.add(it) }
                .onFailure { error ->
                    if (error is TrackPreviewNotFoundException) continue
                    else throw error
                }
        }
        if (rounds.size != config.roundsNumber) {
            throw TrackPreviewCountException(
                foundTrackPreviewCount = rounds.size,
                neededTrackCount = config.roundsNumber
            )
        }
    }

    private suspend fun prepareOneRound(track: Track, allTracksInPlaylist: List<Track>): Result<GameRound> {
        val trackPreview = trackPreviewRepository
            .getPreview(track.artist, track.title)
            .getOrElse { error -> return Result.failure(error) }

        val wrongOptions = mutableListOf<Track>()
        while (wrongOptions.size != WRONG_OPTIONS_NUMBER) {
            val randomTrack = allTracksInPlaylist.random()
            if (randomTrack != track && randomTrack !in wrongOptions) {
                wrongOptions.add(randomTrack)
            }
        }

        return Result.success(GameRound(track, trackPreview, wrongOptions))
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
        val minSnippetLengthInMs = MIN_SNIPPET_LENGTH_IN_SECONDS * 1_000L
        val maxSnippetLengthInMs = MAX_SNIPPET_LENGTH_IN_SECONDS * 1_000L
        require(config.snippetLength in minSnippetLengthInMs..maxSnippetLengthInMs) {
            "Snippet length must be between $minSnippetLengthInMs and $maxSnippetLengthInMs s"
        }
    }

    private fun validatePlaylistAndGetUniqueTracks(playlist: PlaylistContent): List<Track> {
        val uniqueTracks = playlist.tracks.distinctBy { track ->
            track.title to track.artist
        }
        if (uniqueTracks.size < maxOf(config.roundsNumber, WRONG_OPTIONS_NUMBER + 1)) {
            throw TrackCountException(
                actualTrackCount = uniqueTracks.size,
                neededTrackCount = maxOf(config.roundsNumber, WRONG_OPTIONS_NUMBER + 1)
            )
        }
        return uniqueTracks
    }
}
