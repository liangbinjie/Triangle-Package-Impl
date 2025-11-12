; ModuleID = /home/benji/TEC/2025/Compi/Triangle-Package-Impl/ide-triangle-v1.1.src/test.tri
source_filename = "/home/benji/TEC/2025/Compi/Triangle-Package-Impl/ide-triangle-v1.1.src/test.tri"
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-i128:128-f80:128-n8:16:32:64-S128"
target triple = "x86_64-pc-linux-gnu"

; External function declarations
declare i32 @printf(ptr, ...)
declare i32 @putchar(i32)

@.str.putint = private unnamed_addr constant [4 x i8] c"%d\0A\00", align 1
@i = dso_local global i32 0, align 4


; Function Attrs: noinline nounwind optnone uwtable
define dso_local i32 @main() #0 {
entry:
  %1 = alloca i32, align 4
  store i32 0, ptr %1, align 4
  store i32 0, ptr @i, align 4
  br label %L0
L0:
  %t0 = load i32, ptr @i, align 4
  %t1 = icmp slt i32 %t0, 10
  br i1 %t1, label %L1, label %L2
L1:
  %t2 = load i32, ptr @i, align 4
  %t3 = add i32 %t2, 1
  store i32 %t3, ptr @i, align 4
  br label %L0
L2:
  %t4 = load i32, ptr @i, align 4
  %t5 = call i32 (ptr, ...) @printf(ptr @.str.putint, i32 %t4)
  ret i32 0
}

attributes #0 = { noinline nounwind optnone uwtable "frame-pointer"="all" "min-legal-vector-width"="0" "no-trapping-math"="true" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+cmov,+cx8,+fxsr,+mmx,+sse,+sse2,+x87" "tune-cpu"="generic" }

!llvm.module.flags = !{!0, !1, !2, !3, !4}
!llvm.ident = !{!5}

!0 = !{i32 1, !"wchar_size", i32 4}
!1 = !{i32 8, !"PIC Level", i32 2}
!2 = !{i32 7, !"PIE Level", i32 2}
!3 = !{i32 7, !"uwtable", i32 2}
!4 = !{i32 7, !"frame-pointer", i32 2}
!5 = !{!"Triangle LLVM Compiler"}
