package fr.studio.voxel.organ

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.studio.voxel.organ.ui.components.Header
import fr.studio.voxel.organ.components.PrimaryButton
import fr.studio.voxel.organ.ui.components.AnimatedLogo
import fr.studio.voxel.organ.ui.components.Footer
import fr.studio.voxel.organ.ui.theme.OrganTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OrganTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Organ(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Organ(modifier: Modifier = Modifier) {
    AnimatedLogo()
}