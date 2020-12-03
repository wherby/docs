class Cafe{
  def buyCoffee(cc: CreditCard,p: Payment):Coffee={
    val cup = new Coffee()
    p.charge(cc, cup.price)
    cup
  }
}