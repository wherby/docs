
import java.time.format.DateTimeFormatter


import java.time.LocalDate

val date = LocalDate.parse("2021-12-29")
println(DateTimeFormatter.ofPattern("dd-MM-YYYY")
  .format(date))

println(DateTimeFormatter.ofPattern("dd-MM-yyyy")
  .format(date))
println(date)