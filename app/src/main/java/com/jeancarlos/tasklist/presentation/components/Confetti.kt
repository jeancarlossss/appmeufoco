package com.jeancarlos.tasklist.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/**
 * Aqui a gente define o que é UM confete.
 * É como se fosse a "receita" de cada pedacinho de papel que vai cair.
 */
data class ConfettiParticle(
    val color: Color,      // A cor do confete
    val startX: Float,     // Onde ele começa no eixo X (esquerda/direita)
    val startY: Float,     // Onde ele começa no eixo Y (cima/baixo)
    val velocityX: Float,  // A velocidade que ele "corre" para os lados
    val velocityY: Float,  // A velocidade que ele cai
    val rotation: Float,   // O ângulo que ele começa rodado
    val rotationSpeed: Float, // O quão rápido ele fica girando enquanto cai
    val scale: Float,      // O tamanho dele (uns maiores, outros menores)
    val shape: Int         // Se vai ser um círculo (0) ou um quadradinho (1)
)

@Composable
fun ConfettiAnimation(
    modifier: Modifier = Modifier,
    durationMillis: Int = 2500, // Quanto tempo a festa vai durar (2.5 segundos)
    onFinished: () -> Unit     // O que o app faz quando a animação acabar
) {
    // Aqui a gente cria uma lista com 150 confetes diferentes.
    // O 'remember' serve para o Compose não criar tudo de novo toda hora que a tela atualizar.
    val particles = remember {
        List(150) { 
            ConfettiParticle(
                color = listOf(
                    Color(0xFFFFD700), // Dourado
                    Color(0xFFFF69B4), // Rosa
                    Color(0xFF00BFFF), // Azul
                    Color(0xFF32CD32), // Verde
                    Color(0xFFFF4500)  // Laranja
                ).random(), // Escolhe uma cor aleatória da lista
                startX = Random.nextFloat(), // Começa em um ponto aleatório da largura
                startY = Random.nextFloat() * 0.1f - 0.2f, // Começa um pouquinho acima da tela pra "entrar" caindo
                velocityX = (Random.nextFloat() - 0.5f) * 25f, // Joga uns pra esquerda e outros pra direita
                velocityY = Random.nextFloat() * 40f + 20f, // Define a velocidade da queda
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 30f,
                scale = Random.nextFloat() * 0.6f + 0.4f,
                shape = Random.nextInt(2) // Sorteia se é círculo ou quadrado
            )
        }
    }

    // O 'progress' é o nosso cronômetro da animação, ele vai de 0 até 1.
    val progress = remember { Animatable(0f) }

    // O 'LaunchedEffect' faz a mágica começar assim que o componente aparecer na tela.
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis, easing = LinearEasing) // Faz o movimento ser constante
        )
        onFinished() // Avisa que acabou
    }

    // O Canvas é a nossa "lousa" onde vamos desenhar os confetes.
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        // O 'time' ajuda a calcular a posição baseada no progresso da animação.
        val time = progress.value * (durationMillis / 16f)

        particles.forEach { particle ->
            // Simulação de gravidade: quanto mais tempo passa, mais rápido ele cai (time * time)
            val gravity = 0.6f * time * time
            
            // Calcula a posição atual (X e Y) de cada confete
            val x = (particle.startX * width) + (particle.velocityX * time)
            val y = (particle.startY * height) + (particle.velocityY * time) + gravity

            // Só desenha se o confete ainda estiver "dentro" ou quase dentro da tela
            if (y < height + 100 && x in -100f..(width + 100f)) {
                // 'withTransform' permite que a gente rode, mova e mude o tamanho do desenho facilmente
                withTransform({
                    translate(left = x, top = y) // Move para a posição certa
                    rotate(degrees = particle.rotation + (particle.rotationSpeed * time)) // Faz girar
                    scale(scaleX = particle.scale, scaleY = particle.scale) // Ajusta o tamanho
                }) {
                    // Agora sim, desenha a forma sorteada lá no começo
                    if (particle.shape == 0) {
                        drawCircle(
                            color = particle.color,
                            radius = 6.dp.toPx()
                        )
                    } else {
                        drawRect(
                            color = particle.color,
                            topLeft = Offset(-5.dp.toPx(), -5.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(10.dp.toPx(), 10.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}
