package de.menkalian.worklock.ui.navigation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.*
import de.menkalian.worklock.R
import de.menkalian.worklock.controller.*
import de.menkalian.worklock.ui.theme.WorklockTheme
import kotlinx.coroutines.Dispatchers
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DaysView(navController: NavController, viewModel: LogicController = LogicController.get()) {
    val cache = remember { mutableMapOf<LocalDate, Pair<Boolean, Boolean>>() }
    val currentDay = remember { LocalDate.now() }
    val daysOfWeek = remember { daysOfWeek() }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        val state = rememberCalendarState(
            startMonth = YearMonth.of(2024, 1),
            endMonth = currentDay.yearMonth,
            firstVisibleMonth = currentDay.yearMonth,
            firstDayOfWeek = daysOfWeek.first(),
        )

        Text(
            stringResource(R.string.days_select_instruction),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
                .padding(10.dp)
        )

        HorizontalCalendar(
            state = state,
            monthHeader = {
                CalendarTop(month = it.yearMonth, daysOfWeek = daysOfWeek)
            },

            dayContent = { day ->
                key(day.date) {
                    var hasTime by remember { mutableStateOf(false) }
                    var hasError by remember { mutableStateOf(false) }

                    val cached = cache[day.date]
                    if (cached != null) {
                        hasTime = cached.first
                        hasError = cached.second
                    } else {
                        LaunchedEffect(day.date) {
                            val records = viewModel.getAllRecordsForDay(day.date)
                            val (error, time) = getFullTimeForDayInMinutes(records)
                            hasError = error
                            hasTime = time > 0
                            cache[day.date] = hasTime to hasError
                        }
                    }

                    val highlightColor = if (hasError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                    Day(
                        day,
                        highlight = (hasTime || hasError) && day.position == DayPosition.MonthDate,
                        highlightColor = highlightColor,
                        isCurrent = day.date == currentDay,
                        enabled = day.date <= currentDay,
                    ) {
                        navController.navigate("day/${it.date.year}/${it.date.monthValue}/${it.date.dayOfMonth}")
                    }
                }
            }
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Day(
    day: CalendarDay,
    highlight: Boolean,
    highlightAlpha: Float = 0.7f,
    highlightColor: Color = MaterialTheme.colorScheme.primary,
    isCurrent: Boolean = false,
    enabled: Boolean = true,
    onClick: (CalendarDay) -> Unit
) {
    val backgroundColor = if (!enabled) {
        null
    } else if (highlight) {
        highlightColor
    } else if (isCurrent) {
        MaterialTheme.colorScheme.secondary
    } else {
        null
    }
    val contentColor = backgroundColor
        ?.let { contentColorFor(it) }
        ?: LocalContentColor.current

    Box(
        modifier = Modifier
            .aspectRatio(1f) // This is important for square-sizing!
            .padding(6.dp)
            .clip(CircleShape)
            .background(color = backgroundColor?.copy(alpha = highlightAlpha) ?: Color.Transparent)
            // Disable clicks on inDates/outDates
            .clickable(
                enabled = enabled && day.position == DayPosition.MonthDate,
                onClick = { onClick(day) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        val textColor = if (day.position == DayPosition.MonthDate && enabled) {
            contentColor
        } else {
            contentColor.copy(alpha = 0.5f)
                .compositeOver(MaterialTheme.colorScheme.surface)
        }

        Text(
            text = day.date.dayOfMonth.toString(),
            color = textColor,
            fontSize = 14.sp,
            fontWeight = if (isCurrent) {
                FontWeight.Bold
            } else {
                FontWeight.Normal
            },
            textDecoration = if (isCurrent) {
                TextDecoration.Underline
            } else {
                TextDecoration.None
            }
        )
    }
}

@Composable
private fun CalendarTop(
    month: YearMonth,
    daysOfWeek: List<DayOfWeek>,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 14.dp),
                text = month.format(DateTimeFormatter.ofPattern("LLLL yyyy")),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            ) {
                for (dayOfWeek in daysOfWeek) {
                    Text(
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = LocalContentColor.current.copy(alpha = 0.5f)
                            .compositeOver(MaterialTheme.colorScheme.surface),
                        text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        fontSize = 15.sp,
                    )
                }
            }
        }
        HorizontalDivider()
    }
}

@Preview
@Composable
fun DaysPreview() {
    WorklockTheme(darkTheme = false, dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            DaysView(rememberNavController(), DummyLogicController())
        }
    }
}

@Preview
@Composable
fun DaysDarkPreview() {
    WorklockTheme(darkTheme = true, dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            DaysView(rememberNavController(), DummyLogicController())
        }
    }
}