package com.pduvall.whtz.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.pduvall.whtz.R

/** The app title: the Whtz mascot followed by the stylized "Whtz" wordmark, sized to the title. */
@Composable
fun WhtzTitle(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.main_icon),
            contentDescription = null,
            modifier = Modifier.size(96.dp),
        )
        Spacer(Modifier.width(8.dp))
        Image(
            painter = painterResource(R.drawable.whtz_title),
            contentDescription = "Whtz",
            modifier = Modifier.height(44.dp),
        )
    }
}
