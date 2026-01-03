package com.jeancarlos.tasklist.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun LottieCelebration(
    modifier: Modifier = Modifier,
    onFinished: () -> Unit
) {
    // Vou manter o componente Lottie aqui caso você queira adicionar um JSON depois
    // Por enquanto, ele delega para o nosso Confetti vetorial que é muito bonito também.
    ConfettiAnimation(
        modifier = modifier,
        onFinished = onFinished
    )
}

// Versão de carregamento com Lottie (esqueleto)
@Composable
fun LoadingSkeleton() {
    // Simulando um loading bonito com compose puro para não depender de assets externos
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        androidx.compose.material3.CircularProgressIndicator()
    }
}
