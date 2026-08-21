package org.mromichov.guessthesong.domain.game

import android.util.Log
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
    var currentRoundNumber: Int = 0
        private set

    private val rounds = mutableListOf<GameRound>()

    fun rightAnswer(): Track = getCurrentRound().track

    fun getCurrentRound(): GameRound {
        return rounds[currentRoundNumber]
    }

    fun getCurrentTrackPreview(): TrackPreview {
        return rounds[currentRoundNumber].trackPreview
    }

    fun nextRound(): GameRound {
        if (currentRoundNumber != 0) {
            currentRoundNumber++
        }
        return rounds[currentRoundNumber]
    }

    suspend fun prepareRounds(playlist: Playlist) {
        repeat(5) {
            val track = playlist.tracks.random()

            val round = prepareOneRound(track, playlist.tracks)
            rounds.add(round)
        }
    }

    private suspend fun prepareOneRound(track: Track, allTracksInPlaylist: List<Track>): GameRound {
        val wrongOptions = mutableListOf<Track>()
        while (wrongOptions.size != 3) {
            val randomTrack = allTracksInPlaylist.random()
            if (randomTrack != track) {
                wrongOptions.add(randomTrack)
            }
        }
        val trackPreview: TrackPreview = trackPreviewRepository
            .getPreview(track.artist, track.title)
            .onFailure { error ->
                Log.e("TrackPreview", "Ошибка получения трека: ${error.message}", error)
            }

            .getOrElse {
                throw TrackPreviewException()
            }


        return GameRound(track, trackPreview, wrongOptions)
    }
}