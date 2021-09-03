@foobar(
  "annot", {
    val x = 2
    val y = 2 // y=2
    x + y
  }
)
object a extends b with c {
  def foo[T: Int#Double#Triple, R <% String](
      @annot1
      x: Int @annot2 = 2,
      y: Int = 3
  ): Int = {
    "match" match {
      case 1 | 2 =>
        3
      case <A>2</A> => 2
    }
  }
}
