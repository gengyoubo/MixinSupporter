# @Mixin
@Mixin(XX.class)
# @Shadow
@Shadow XX
需在XX.class寻找
# @Unique
code();
# @Overwrite
@Overwrite
XX(){}
需在XX.class寻找
# @Inject
@Inject(method = "", at = @At(value = "HEAD/RETURN/TAIL",shift =At.Shift.AFTER/At.Shift.BEFORE)
@Inject(method = "", at = @At(value = "HEAD/RETURN/TAIL",shift =At.Shift.BY ,by = )
@Inject(method = "", at = @At(value = "INVOKE",target = "" )
# @ModifyArg
@ModifyArg(method = "", at = @At(value = "HEAD/RETURN/TAIL",shift =At.Shift.AFTER/At.Shift.BEFORE)
@ModifyArg(method = "", at = @At(value = "HEAD/RETURN/TAIL",shift =At.Shift.BY ,by = )
@ModifyArg(method = "", at = @At(value = "INVOKE",target = "" )
# @ModifyArgs
@ModifyArgs(method = "", at = @At(value = "HEAD/RETURN/TAIL",shift =At.Shift.AFTER/At.Shift.BEFORE)
@ModifyArgs(method = "", at = @At(value = "HEAD/RETURN/TAIL",shift =At.Shift.BY ,by = )
@ModifyArgs(method = "", at = @At(value = "INVOKE",target = "" )
# @ModifyVariable
@ModifyVariable(method = "", at = @At(value = "HEAD/RETURN/TAIL",shift =At.Shift.AFTER/At.Shift.BEFORE)
@ModifyVariable(method = "", at = @At(value = "HEAD/RETURN/TAIL",shift =At.Shift.BY ,by = )
@ModifyVariable(method = "", at = @At(value = "INVOKE",target = "" )
# @Redirect
@Redirect(method = "", at = @At(value = "HEAD/RETURN/TAIL",shift =At.Shift.AFTER/At.Shift.BEFORE)
@Redirect(method = "", at = @At(value = "HEAD/RETURN/TAIL",shift =At.Shift.BY ,by = )
@Redirect(method = "", at = @At(value = "INVOKE",target = "" )