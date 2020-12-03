class Cafe{
  def buyCoffee(cc: CreditCard):(Coffee,Charge)={
    val cup = new Coffee()
    (cup,Charge(cc,cup.price))
  }
}

case class Charge(cc: CreditCard,amount:Double){
  def combine(other:Charge):Charge={
    if(cc == other.cc)
      Charge(cc, amount + other.amount)
  }
}