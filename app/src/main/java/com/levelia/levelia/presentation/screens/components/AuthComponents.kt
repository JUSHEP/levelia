package com.levelia.levelia.presentation.screens.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.levelia.levelia.ui.theme.MoradoAcento
import com.levelia.levelia.ui.theme.MoradoInput
import com.levelia.levelia.ui.theme.PlaceholderInput
import com.levelia.levelia.ui.theme.TextoSecundario
import com.levelia.levelia.ui.theme.TextoSobreClaro

/**
 * Contenedor común de las pantallas de auth: scroll, espacio para el teclado, flecha atrás
 * opcional e indicador "N de 4".
 */
@Composable
fun AuthColumn(
    title: String,
    paso: Int? = null,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().imePadding()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 56.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (paso != null) {
                Text(
                    "$paso de 4",
                    color = MoradoAcento,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            content()
        }
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.padding(4.dp).align(Alignment.TopStart)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
        }
    }
}

/** Input con etiqueta encima (blanca sobre el fondo oscuro) y caja morada clara con texto oscuro. */
@Composable
fun AuthTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supportingText: String? = null,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = true,
            placeholder = placeholder?.let { { Text(it) } },
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            trailingIcon = trailingIcon,
            shape = RoundedCornerShape(12.dp),
            colors = authTextFieldColors(),
        )
        if (supportingText != null) {
            Text(supportingText, style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
        }
    }
}

@Composable
fun authTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MoradoInput,
    unfocusedContainerColor = MoradoInput,
    disabledContainerColor = MoradoInput.copy(alpha = 0.6f),
    focusedTextColor = TextoSobreClaro,
    unfocusedTextColor = TextoSobreClaro,
    disabledTextColor = TextoSobreClaro.copy(alpha = 0.6f),
    focusedBorderColor = MoradoAcento,
    unfocusedBorderColor = MoradoInput,
    cursorColor = TextoSobreClaro,
    focusedPlaceholderColor = PlaceholderInput,
    unfocusedPlaceholderColor = PlaceholderInput,
    focusedTrailingIconColor = TextoSobreClaro,
    unfocusedTrailingIconColor = TextoSobreClaro,
)

@Composable
fun ErrorMessage(error: String?) {
    if (error != null) {
        Text(
            error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun LoadingButton(text: String, isLoading: Boolean, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            Text(text, fontWeight = FontWeight.SemiBold)
        }
    }
}
