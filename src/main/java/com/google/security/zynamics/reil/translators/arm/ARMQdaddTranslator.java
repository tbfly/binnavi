/*
Copyright 2011-2016 Google Inc. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.google.security.zynamics.reil.translators.arm;

import com.google.security.zynamics.reil.OperandSize;
import com.google.security.zynamics.reil.ReilHelpers;
import com.google.security.zynamics.reil.ReilInstruction;
import com.google.security.zynamics.reil.translators.ITranslationEnvironment;
import com.google.security.zynamics.reil.translators.InternalTranslationException;
import com.google.security.zynamics.reil.translators.TranslationHelpers;
import com.google.security.zynamics.zylib.disassembly.IInstruction;
import com.google.security.zynamics.zylib.disassembly.IOperandTreeNode;

import java.util.List;


public class ARMQdaddTranslator extends ARMBaseTranslator {
  @Override
  protected void translateCore(final ITranslationEnvironment environment,
      final IInstruction instruction, final List<ReilInstruction> instructions) {
    final IOperandTreeNode registerOperand1 =
        instruction.getOperands().get(0).getRootNode().getChildren().get(0);
    final IOperandTreeNode registerOperand2 =
        instruction.getOperands().get(1).getRootNode().getChildren().get(0);
    final IOperandTreeNode registerOperand3 =
        instruction.getOperands().get(2).getRootNode().getChildren().get(0);

    final String targetRegister = (registerOperand1.getValue());
    final String sourceRegister1 = (registerOperand2.getValue());
    final String sourceRegister2 = (registerOperand3.getValue());

    final OperandSize dw = OperandSize.DWORD;
    final OperandSize bt = OperandSize.BYTE;

    long baseOffset = (instruction.getAddress().toLong() * 0x100) + instructions.size();

    final String shiftedRn = environment.getNextVariableString();
    final String signedDoesSat1 = environment.getNextVariableString();
    final String signedDoesSat2 = environment.getNextVariableString();
    final String tmpResultVar1 = environment.getNextVariableString();
    final String added2RnRm = environment.getNextVariableString();

    instructions.add(ReilHelpers.createBsh(baseOffset++, dw, sourceRegister2, bt,
        String.valueOf(1), dw, shiftedRn));

    Helpers.signedSat(baseOffset, environment, instruction, instructions, dw, sourceRegister2, dw,
        sourceRegister2, dw, shiftedRn, "ADD", tmpResultVar1, 32L, signedDoesSat1);

    instructions.add(ReilHelpers.createAdd(baseOffset++, dw, tmpResultVar1, dw, sourceRegister1,
        dw, added2RnRm));

    Helpers.signedSat(baseOffset, environment, instruction, instructions, dw, tmpResultVar1, dw,
        sourceRegister1, dw, added2RnRm, "ADD", targetRegister, 32L, signedDoesSat2);

    instructions.add(ReilHelpers.createOr(baseOffset++, bt, signedDoesSat1, bt, signedDoesSat2, bt,
        "Q"));
  }

  /**
   * QDADD{<cond>} <Rd>, <Rm>, <Rn>
   * 
   * Operation:
   * 
   * if ConditionPassed(cond) then Rd = SignedSat(Rm + SignedSat(Rn*2, 32), 32) if SignedDoesSat(Rm
   * + SignedSat(Rn*2, 32), 32) or SignedDoesSat(Rn*2, 32) then Q Flag = 1
   */

  @Override
  public void translate(final ITranslationEnvironment environment, final IInstruction instruction,
      final List<ReilInstruction> instructions) throws InternalTranslationException {
    TranslationHelpers.checkTranslationArguments(environment, instruction, instructions, "QDADD");
    translateAll(environment, instruction, "QDADD", instructions);
  }
}
