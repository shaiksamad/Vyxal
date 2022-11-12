package vyxal

object Interpreter {
  def execute(code: String)(using ctx: Context): Unit = {
    VyxalParser.parse(code) match {
      case Right(ast) =>
        println(ast)
        execute(ast)
        // todo implicit output according to settings
        if (!ctx.isStackEmpty) {
          println(ctx.peek)
        }
      case Left(error) =>
        println(error)
        ???
    }
  }

  def execute(ast: AST)(using ctx: Context): Unit = {
    println(s"Executing $ast")
    ast match {
      case AST.Number(value) => ctx.push(value)
      case AST.Str(value)    => ctx.push(value)
      case AST.Lst(elems) =>
        val list = collection.mutable.ListBuffer.empty[VAny]
        for (elem <- elems) {
          execute(elem)
          list += ctx.pop()
        }
        ctx.push(VList(list.toList*))
      case AST.Command(cmd) =>
        Elements.elements.get(cmd) match {
          case Some(elem) => elem.impl()
          case None       => throw RuntimeException(s"No such command: '$cmd'")
        }
      case AST.Group(elems) =>
        elems.foreach { elem => Interpreter.execute(elem) }
      case AST.If(thenBody, elseBody) =>
        if (MiscHelpers.boolify(ctx.pop())) {
          execute(thenBody)
        } else if (elseBody.nonEmpty) {
          execute(elseBody.get)
        }
      case AST.While(None, body) =>
        val loopCtx = ctx.makeChild()
        loopCtx.contextVar = ctx.settings.rangeStart
        while (true) {
          execute(body)(using loopCtx)
          loopCtx.contextVar match {
            case temp: VNum =>
              ctx.contextVar = temp + 1
            case _ =>
              // todo is this even possible? Maybe we can just cast
              throw RuntimeException(
                "Non-numeric value pushed onto context stack in while loop"
              )
          }
        }
      case AST.While(Some(cond), body) =>
        execute(cond)
        given loopCtx: Context = ctx.makeChild()
        loopCtx.contextVar = ctx.settings.rangeStart
        while (MiscHelpers.boolify(ctx.pop())) {
          execute(body)
          execute(cond)
          loopCtx.contextVar match {
            case prev: VNum =>
              loopCtx.contextVar = prev + 1
            case _ =>
              throw RuntimeException(
                "Non-numeric value pushed onto context stack in while loop"
              )
          }
        }

      case AST.For(None, body) =>
        val iterable = ListHelpers.makeIterable(ctx.pop(), ctx)
        given loopCtx: Context = ctx.makeChild()
        for (elem <- iterable) {
          loopCtx.contextVar = elem
          execute(body)(using loopCtx)
        }

      case AST.For(Some(name), body) =>
        println(name)
        val iterable = ListHelpers.makeIterable(ctx.pop(), ctx)
        given loopCtx: Context = ctx.makeChild()
        for (elem <- iterable) {
          loopCtx.setVar(name, elem)
          loopCtx.contextVar = elem
          execute(body)(using loopCtx)
        }

      case AST.GetVar(name) => ctx.push(ctx.getVar(name))
      case AST.SetVar(name) => ctx.setVar(name, ctx.pop())
    }
  }
}
