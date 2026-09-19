package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.auth.AuthState
import com.example.ui.theme.LocalViraExtraColors
import com.example.ui.theme.ViraRadius
import com.example.ui.theme.ViraSpacing
import com.example.ui.theme.ViraTypography

private enum class AuthMode {
    SIGN_IN,
    SIGN_UP,
    FORGOT_PASSWORD
}

@Composable
fun AuthDialog(
    authState: AuthState,
    onDismiss: () -> Unit,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit,
    onResetPassword: ((String) -> Unit)? = null,
    isSignInDefault: Boolean = false
) {
    var mode by remember(isSignInDefault) { mutableStateOf(if (isSignInDefault) AuthMode.SIGN_IN else AuthMode.SIGN_UP) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var resetEmailSent by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ViraSpacing.space24)
                .clip(RoundedCornerShape(ViraRadius.large)),
            color = LocalViraExtraColors.current.cardBackground,
            border = BorderStroke(1.dp, LocalViraExtraColors.current.cardBorder),
            shape = RoundedCornerShape(ViraRadius.large)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ViraSpacing.space24),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Vira Icon Emblem
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(ViraSpacing.space16))

                // Headline
                Text(
                    text = "O teu histórico é teu.",
                    style = ViraTypography.ScreenTitle,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(ViraSpacing.space8))

                // Supporting Copy
                Text(
                    text = when (mode) {
                        AuthMode.SIGN_UP -> "Guarda as tuas recolhas em segurança e sincroniza os teus dados entre telemóveis."
                        AuthMode.SIGN_IN -> "Acede à tua conta para restaurar as tuas recolhas e estatísticas."
                        AuthMode.FORGOT_PASSWORD -> "Introduz o teu email para redefinir a tua palavra-passe."
                    },
                    style = ViraTypography.BodySecondary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(ViraSpacing.space24))

                // Error Banner if present
                if (authState is AuthState.Error) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(ViraRadius.small)
                    ) {
                        Text(
                            text = authState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = ViraTypography.Caption,
                            modifier = Modifier.padding(horizontal = ViraSpacing.space12, vertical = ViraSpacing.space8),
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(ViraSpacing.space16))
                }

                if (mode == AuthMode.FORGOT_PASSWORD) {
                    if (resetEmailSent) {
                        Text(
                            text = "Email de recuperação enviado para $email. Verifica a tua caixa de correio.",
                            color = MaterialTheme.colorScheme.primary,
                            style = ViraTypography.BodySecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(ViraSpacing.space24))
                        ViraHeroButton(
                            text = "Concluir",
                            onClick = onDismiss
                        )
                    } else {
                        ViraTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = "Email",
                            placeholder = "o-teu-email@exemplo.com",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(ViraSpacing.space24))
                        ViraHeroButton(
                            text = "Enviar email de recuperação",
                            onClick = {
                                if (email.isNotBlank()) {
                                    onResetPassword?.invoke(email.trim())
                                    resetEmailSent = true
                                }
                            },
                            enabled = email.isNotBlank() && email.contains("@") && authState !is AuthState.Loading
                        )
                    }
                    Spacer(modifier = Modifier.height(ViraSpacing.space12))
                    TextButton(
                        onClick = {
                            mode = AuthMode.SIGN_IN
                            resetEmailSent = false
                        }
                    ) {
                        Text(
                            text = "Voltar ao início de sessão",
                            style = ViraTypography.ButtonLabel,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    ViraTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email",
                        placeholder = "o-teu-email@exemplo.com",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(ViraSpacing.space12))

                    ViraTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Palavra-passe",
                        placeholder = "Pelo menos 6 caracteres",
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (mode == AuthMode.SIGN_IN) {
                        Spacer(modifier = Modifier.height(ViraSpacing.space4))
                        TextButton(
                            onClick = {
                                mode = AuthMode.FORGOT_PASSWORD
                                resetEmailSent = false
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                text = "Esqueci-me da palavra-passe",
                                style = ViraTypography.Caption,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(ViraSpacing.space24))

                    // HERO ACTION
                    val actionText = when {
                        authState is AuthState.Loading -> "A processar..."
                        mode == AuthMode.SIGN_UP -> "Criar conta"
                        else -> "Iniciar sessão"
                    }
                    ViraHeroButton(
                        text = actionText,
                        onClick = {
                            if (mode == AuthMode.SIGN_UP) onSignUp(email.trim(), password)
                            else onSignIn(email.trim(), password)
                        },
                        enabled = email.isNotBlank() && password.length >= 6 && authState !is AuthState.Loading
                    )

                    Spacer(modifier = Modifier.height(ViraSpacing.space12))

                    // SECONDARY: Toggle Sign Up / Sign In
                    TextButton(
                        onClick = {
                            mode = if (mode == AuthMode.SIGN_UP) AuthMode.SIGN_IN else AuthMode.SIGN_UP
                        }
                    ) {
                        Text(
                            text = if (mode == AuthMode.SIGN_UP) "Já tenho conta" else "Criar nova conta",
                            style = ViraTypography.ButtonLabel,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // TERTIARY: Continue without account
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Continuar sem conta",
                            style = ViraTypography.Caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
