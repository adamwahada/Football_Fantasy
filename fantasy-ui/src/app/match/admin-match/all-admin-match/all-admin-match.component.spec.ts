import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AllAdminMatchComponent } from './all-admin-match.component';

describe('AllAdminMatchComponent', () => {
  let component: AllAdminMatchComponent;
  let fixture: ComponentFixture<AllAdminMatchComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AllAdminMatchComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AllAdminMatchComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
