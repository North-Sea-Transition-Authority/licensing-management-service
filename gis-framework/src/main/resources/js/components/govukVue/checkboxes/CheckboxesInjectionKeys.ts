import type { InjectionKey, Ref } from 'vue'

export const CheckboxesModelValueInjectionKey: InjectionKey<Ref<Array<any> | undefined>> =
  Symbol('CheckboxesModelValue')

export const CheckboxesUpdateModelValueFunctionInjectionKey: InjectionKey<Function> = Symbol(
  'CheckboxesUpdateModelValueFunction'
)

export const CheckboxesNameInjectionKey: InjectionKey<Ref<string>> = Symbol('CheckboxesName')