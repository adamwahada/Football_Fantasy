import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UserGameweekFinishedComponent } from './user-gameweek-finished.component';

describe('UserGameweekFinishedComponent', () => {
  let component: UserGameweekFinishedComponent;
  let fixture: ComponentFixture<UserGameweekFinishedComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserGameweekFinishedComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(UserGameweekFinishedComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
