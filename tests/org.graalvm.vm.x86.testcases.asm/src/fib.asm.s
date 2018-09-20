	.text

        .global	_start
_start:
	# x = fib(11)
	mov	$11,	%rdi
	call	fib
	mov	%rax,	%rdi

	# exit(x)
	mov	$60,	%eax
	syscall

fib:	mov	%rdi,	%rsi	# rsi = n
	xor	%rax,	%rax	# rax = t0
	mov	$1,	%rdx	# rdx = t1
.L0:	xchg	%rax,	%rdx	# rdx = t0, rax = t1
	add	%rax,	%rdx	# rdx = t2, rax = t1
	dec	%rsi
	jnz	.L0
	ret
